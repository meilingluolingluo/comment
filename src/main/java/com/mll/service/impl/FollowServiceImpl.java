package com.mll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mll.dto.Result;
import com.mll.dto.UserDTO;
import com.mll.entity.Follow;
import com.mll.mapper.FollowMapper;
import com.mll.service.IFollowService;
import org.springframework.stereotype.Service;

import static com.mll.utils.UserHolder.getUser;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = getUser().getId();
       if (isFollow){
           Follow follow = new Follow();
           follow.setUserId(userId);
           follow.setFollowUserId(followUserId);
           return save(follow) ? Result.ok() : Result.fail("关注失败");
       }else {
           boolean removed = remove(new QueryWrapper<Follow>()
                   .eq("user_id",userId)
                   .eq("follow_user_id",followUserId));
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
        return null;
    }

    // 封装查询逻辑到单独的方法中
    private Long queryFollowCount(Long userId, Long followUserId) {
        return query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
    }

}
