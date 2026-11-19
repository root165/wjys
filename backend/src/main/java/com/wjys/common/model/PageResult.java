package com.wjys.common.model;

import java.util.List;

public record PageResult<T>(long total, long pageNum, long pageSize, List<T> list) {
}
