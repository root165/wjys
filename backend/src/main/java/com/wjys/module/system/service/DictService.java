package com.wjys.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.module.system.dto.SaveDictRequest;
import com.wjys.module.system.entity.SysDict;
import com.wjys.module.system.mapper.SysDictMapper;
import com.wjys.module.system.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import com.wjys.common.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictService {
    private final SysDictMapper dictMapper;

    public List<DictVO> list(String dictType) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>().eq(SysDict::getDictType, dictType).eq(SysDict::getStatus, 1).orderByAsc(SysDict::getSortNo, SysDict::getId)).stream().map(this::toVO).toList();
    }

    @Transactional
    public DictVO create(SaveDictRequest request) {
        validateUnique(request, null);
        SysDict dict = new SysDict();
        dict.setDictType(request.dictType());
        dict.setDictLabel(request.dictLabel());
        dict.setDictValue(request.dictValue());
        dict.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        dict.setStatus(1);
        dictMapper.insert(dict);
        return toVO(dict);
    }

    @Transactional
    public DictVO update(Long id, SaveDictRequest request) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) throw new BusinessException(HttpStatus.NOT_FOUND, "字典项不存在");
        validateUnique(request, id);
        dict.setDictType(request.dictType());
        dict.setDictLabel(request.dictLabel());
        dict.setDictValue(request.dictValue());
        dict.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        dictMapper.updateById(dict);
        return toVO(dict);
    }

    @Transactional
    public void delete(Long id) {
        if (dictMapper.selectById(id) == null) throw new BusinessException(HttpStatus.NOT_FOUND, "字典项不存在");
        dictMapper.deleteById(id);
    }

    private void validateUnique(SaveDictRequest request, Long selfId) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, request.dictType()).eq(SysDict::getDictValue, request.dictValue());
        if (selfId != null) wrapper.ne(SysDict::getId, selfId);
        if (dictMapper.selectCount(wrapper) > 0) throw new BusinessException(HttpStatus.CONFLICT, "同类型字典值已存在");
    }

    private DictVO toVO(SysDict dict) { return new DictVO(dict.getId(), dict.getDictType(), dict.getDictLabel(), dict.getDictValue(), dict.getSortNo(), dict.getStatus()); }
}
