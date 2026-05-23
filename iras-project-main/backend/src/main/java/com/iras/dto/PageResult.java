/**
 * @file PageResult.java
 * @description 分页查询结果封装类。
 *              用于封装分页查询的返回数据，包含当前页数据列表、
 *              总记录数、当前页码、每页大小和总页数。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页查询结果封装。
 * <p>
 * 泛型参数 T 表示列表中每条记录的类型。
 * 用于统一所有分页查询接口的返回格式。
 * </p>
 *
 * @param <T> 记录数据的类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    /** 当前页的数据列表 */
    private List<T> records;

    /** 符合条件的总记录数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页显示的记录数 */
    private int size;

    /** 总页数（根据 total 和 size 计算得出） */
    private int totalPages;
}
