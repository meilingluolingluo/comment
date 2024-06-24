package com.mll.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.dto.UserDTO;
import com.mll.entity.Follow;
import com.mll.entity.User;
import com.mll.mapper.FollowMapper;
import com.mll.service.IFollowService;
import com.mll.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mll.utils.RedisConstants.USER_FOLLOW_KEY;
import static com.mll.utils.UserHolder.getUser;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    private final StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    public FollowServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = getUser().getId();
       if (isFollow){
           Follow follow = new Follow();
           follow.setUserId(userId);
           follow.setFollowUserId(followUserId);
           boolean successSave = save(follow);
           if(successSave){
               stringRedisTemplate.opsForSet().add(USER_FOLLOW_KEY  + userId,followUserId.toString());
               return Result.ok();
           }
           return Result.fail("关注失败");
           //return save(follow) ? Result.ok() : Result.fail("关注失败");
       }else {
           boolean removed = remove(new QueryWrapper<Follow>()
                   .eq("user_id",userId)
                   .eq("follow_user_id",followUserId));
           stringRedisTemplate.opsForSet().remove(USER_FOLLOW_KEY  + userId,followUserId.toString());
           return removed ? Result.ok() : Result.fail("取消关注失败");
       }
    }

    @Override
    public Result isFollow(Long followUserId) {
        // 检查传入的 followUserId 是否为 null
        if (followUserId == null) {
            return Result.fail("关注用户ID不能为空");
        }

        // 获取当前用户
        UserDTO currentUser = getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }
        Long userId = currentUser.getId();

        // 检查是否已关注
        try {
            Long count = queryFollowCount(userId, followUserId);
            return count > 0 ? Result.ok(true) : Result.ok(false);
        } catch (Exception e) {
            // 捕获可能的异常并返回错误信息
            return Result.fail("查询关注状态时发生错误");
        }
    }


    @Override
    public Result followCommons(Long id) {
        // 检查传入的 id 是否为 null
        if (id == null) {
            return Result.fail("用户ID不能为空");
        }

        // 获取当前用户
        UserDTO currentUser = getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }
        Long userId = currentUser.getId();

        // 获取 Redis 中的关注集合交集
        try {
            Set<String> intersect = stringRedisTemplate.opsForSet().intersect(USER_FOLLOW_KEY + userId, USER_FOLLOW_KEY + id);
            if (intersect != null && !intersect.isEmpty()) {
                // 直接从交集中获取 UserDTO 列表
                List<UserDTO> users = intersect.stream()
                        .map(Long::valueOf)
                        .map(userService::getById)
                        .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                        .collect(Collectors.toList());
                return Result.ok(users);
            }
        } catch (Exception e) {
            // 捕获可能的异常并返回错误信息
            return Result.fail("获取共同关注用户时发生错误");
        }

        // 返回结果
        return Result.ok();
    }

    // 封装查询逻辑到单独的方法中
    private Long queryFollowCount(Long userId, Long followUserId) {
        return query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
    }

}
