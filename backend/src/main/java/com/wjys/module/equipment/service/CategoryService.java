package com.wjys.module.equipment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.dto.SaveCategoryRequest;
import com.wjys.module.equipment.entity.EqCategory;
import com.wjys.module.equipment.mapper.EqCategoryMapper;
import com.wjys.module.equipment.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final EqCategoryMapper categoryMapper;

    public List<CategoryVO> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EqCategory>().eq(EqCategory::getStatus, 1).orderByAsc(EqCategory::getSortNo, EqCategory::getId)).stream().map(this::toVO).toList();
    }

    @Transactional
    public CategoryVO create(SaveCategoryRequest request) {
        long parentId = request.parentId() == null ? 0 : request.parentId();
        validateParent(parentId, null);
        EqCategory category = new EqCategory();
        category.setParentId(parentId); category.setCategoryName(request.categoryName()); category.setSortNo(request.sortNo() == null ? 0 : request.sortNo()); category.setStatus(1);
        categoryMapper.insert(category);
        return toVO(category);
    }

    @Transactional
    public CategoryVO update(Long id, SaveCategoryRequest request) {
        EqCategory category = getRequired(id);
        long parentId = request.parentId() == null ? 0 : request.parentId();
        validateParent(parentId, id);
        category.setParentId(parentId);
        category.setCategoryName(request.categoryName());
        category.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        categoryMapper.updateById(category);
        return toVO(category);
    }

    @Transactional
    public void delete(Long id) {
        getRequired(id);
        if (categoryMapper.countChildren(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "存在下级设备分类，不能删除");
        if (categoryMapper.countActiveEquipments(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "分类下存在在用设备，不能删除");
        categoryMapper.deleteById(id);
    }

    private EqCategory getRequired(Long id) {
        EqCategory category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException(HttpStatus.NOT_FOUND, "设备分类不存在");
        return category;
    }

    private void validateParent(long parentId, Long selfId) {
        if (parentId == 0) return;
        if (selfId != null && parentId == selfId) throw new BusinessException(HttpStatus.BAD_REQUEST, "设备分类不能设置自身为上级");
        if (categoryMapper.selectById(parentId) == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "上级分类不存在");
    }

    private CategoryVO toVO(EqCategory category) { return new CategoryVO(category.getId(), category.getParentId(), category.getCategoryName(), category.getSortNo(), category.getStatus()); }
}
