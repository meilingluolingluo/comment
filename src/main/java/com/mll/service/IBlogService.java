package com.mll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mll.dto.Result;
import com.mll.entity.Blog;

public interface IBlogService extends IService<Blog> {
    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result queryBlogLikes(Long id);

    Result queryBlogOfFollow(Long max, Integer offset);

    Result saveBlog(Blog blog);

    Result likeBlog(Long id);
}
