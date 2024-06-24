package com.mll.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.dto.UserDTO;
import com.mll.entity.Blog;
import com.mll.entity.User;
import com.mll.mapper.BlogMapper;
import com.mll.service.IBlogService;
import com.mll.service.IUserService;
import com.mll.utils.SystemConstants;
import com.mll.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.mll.utils.RedisConstants.BLOG_LIKED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryBlogLikes(Long id) {
        Set<String> ids = stringRedisTemplate.opsForZSet().reverseRange(BLOG_LIKED_KEY + id, 0, 4);
        if (ids == null || ids.isEmpty()) {
            return Result.ok();
        }
        System.out.println("ids: "+ids);
        return Result.ok(ids);
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        return Result.ok();
    }

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        if(save(blog))
            return Result.ok(blog.getId());
        return Result.fail("笔记发布失败！");
    }

    @Override
    public Result likeBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        String key = BLOG_LIKED_KEY + id;
        String memberId = user.getId().toString();

        Double isLiked = stringRedisTemplate.opsForZSet().score(key, memberId);

        if (isLiked == null) {
            // 如果未点赞，可以点赞
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id",id).update();
            // 保存用户到Redis的set集合 zadd(key, score, member)
           if(isSuccess) {
               stringRedisTemplate.opsForZSet().add(key, memberId, System.currentTimeMillis());
               return Result.ok();
           }
           return Result.fail("点赞失败");

        } else {
            // 如果已点赞，取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id",id).update();
           if(isSuccess) {
               stringRedisTemplate.opsForZSet().remove(key, memberId);
               return Result.ok("取消赞");
           }
           return Result.fail("取消赞失败");
        }


    }
    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
