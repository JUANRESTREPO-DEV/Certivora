package com.eduessence.sendmail.repository;

import com.eduessence.sendmail.model.entity.EmailTemplateVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailTemplateVariableRepository extends JpaRepository<EmailTemplateVariable, Long> {
    List<EmailTemplateVariable> findAllByTemplate_Id(Long templateId);
}
