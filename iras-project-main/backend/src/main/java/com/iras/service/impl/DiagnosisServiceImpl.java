/**
 * @file DiagnosisServiceImpl.java
 * @description 诊断历史记录服务实现类。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.service.impl;

import com.iras.dto.PageResult;
import com.iras.entity.DiagnosisRecord;
import com.iras.mapper.DiagnosisMapper;
import com.iras.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    private final DiagnosisMapper diagnosisMapper;

    @Override
    public void saveRecord(DiagnosisRecord record) {
        diagnosisMapper.insert(record);
    }

    @Override
    public PageResult<DiagnosisRecord> getHistory(Long userId, int page, int size) {
        int offset = (page - 1) * size;

        List<DiagnosisRecord> records = diagnosisMapper.findByUserId(userId, offset, size);
        long total = diagnosisMapper.countByUserId(userId);

        PageResult<DiagnosisRecord> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }

    @Override
    public DiagnosisRecord getDetail(Long id, Long userId) {
        DiagnosisRecord record = diagnosisMapper.findById(id);
        // 校验记录归属，防止越权访问
        if (record != null && !record.getUserId().equals(userId)) {
            return null;
        }
        return record;
    }

    @Override
    public boolean deleteRecord(Long id, Long userId) {
        DiagnosisRecord record = diagnosisMapper.findById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return false;
        }
        return diagnosisMapper.deleteById(id) > 0;
    }
}
