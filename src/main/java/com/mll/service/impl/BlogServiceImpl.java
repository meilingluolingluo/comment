package com.mll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.dto.ScrollResult;
import com.mll.dto.UserDTO;
import com.mll.entity.Blog;
import com.mll.entity.Follow;
import com.mll.entity.User;
import com.mll.mapper.BlogMapper;
import com.mll.service.IBlogService;
import com.mll.service.IFollowService;
import com.mll.service.IUserService;
import com.mll.utils.SystemConstants;
import com.mll.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.mll.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.mll.utils.RedisConstants.USER_FOLLOW_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private IFollowService followService;
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
    //滚动查询
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "feed:" + userId;

        // 确保 key 的类型是 ZSet
        String keyType = String.valueOf(stringRedisTemplate.type(key));
        if (!"zset".equals(keyType)) {
            System.out.println("Key type mismatch: expected ZSet, but found " + keyType);
            return Result.fail("Key type mismatch: expected ZSet, but found " + keyType);
        }
        // 查询收件箱
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 5);

        // 检查查询结果是否为空
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }

        // 提取博客ID列表
        List<Long> blogIds = new ArrayList<>(typedTuples.size());
        long minTime = Long.MAX_VALUE;
        int newOffset = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            blogIds.add(Long.valueOf(Objects.requireNonNull(typedTuple.getValue())));
            long time = Objects.requireNonNull(typedTuple.getScore()).longValue();
            if (time == minTime) {
                newOffset++;
            } else {
                minTime = time;
                newOffset = 1;
            }
        }
        List<Blog> blogs = query().in("id", blogIds).list();
        // 按照ID顺序排序
        blogs.sort((b1, b2) -> {
            int index1 = blogIds.indexOf(b1.getId());
            int index2 = blogIds.indexOf(b2.getId());
            return index1 - index2;
        });
        for(Blog blog : blogs){
            queryBlogUser(blog);
            isBlogLiked(blog);
        }
        // 返回结果和新的offset
        ScrollResult result = new ScrollResult();
        result.setList(blogs);
        result.setOffset(newOffset);
        result.setMinTime(minTime);

        return Result.ok(result);
    }

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        if(save(blog)){

            return Result.ok(blog.getId());
        }
        List<Follow> follows = followService.query().eq("followUserId", user.getId()).eq("followUserId", user.getId()).list();
        for(Follow follow : follows){
            Long userId = follow.getUserId();
            String key = "feed:" + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }

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
