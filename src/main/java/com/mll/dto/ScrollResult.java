package com.mll.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrollResult {
    private Long minTime;
    private Integer offset;
    private List<?> list;

}
