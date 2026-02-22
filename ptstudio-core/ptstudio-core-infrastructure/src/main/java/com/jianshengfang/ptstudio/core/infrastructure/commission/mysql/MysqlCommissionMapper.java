package com.jianshengfang.ptstudio.core.infrastructure.commission.mysql;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface MysqlCommissionMapper {

    @Select("""
            SELECT id, tenant_id, store_id, rule_code, rule_name, calc_mode,
                   CAST(rule_json AS CHAR) AS ruleJson, version, effective_from, effective_to,
                   status, created_at, updated_at
            FROM t_commission_rule
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
            ORDER BY id
            """)
    List<MysqlCommissionPo.RulePo> listRules(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Select("""
            SELECT COALESCE(MAX(version), 0)
            FROM t_commission_rule
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    int maxRuleVersion(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO t_commission_rule(tenant_id, store_id, rule_code, rule_name, calc_mode, rule_json,
                                          version, effective_from, effective_to, status, created_by, created_at, updated_at)
            VALUES(#{tenantId}, #{storeId}, #{ruleCode}, #{ruleName}, #{calcMode}, #{ruleJson},
                   #{version}, #{effectiveFrom}, #{effectiveTo}, #{status}, 0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRule(MysqlCommissionPo.RulePo po);

    @Select("""
            SELECT id, tenant_id, store_id, rule_code, rule_name, calc_mode,
                   CAST(rule_json AS CHAR) AS ruleJson, version, effective_from, effective_to,
                   status, created_at, updated_at
            FROM t_commission_rule
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    MysqlCommissionPo.RulePo getRule(@Param("id") Long id,
                                     @Param("tenantId") Long tenantId,
                                     @Param("storeId") Long storeId);

    @Select("""
            SELECT id, tenant_id, store_id, statement_no, statement_month, coach_id, rule_id,
                   gross_amount, commission_amount, status, locked_at, created_at, updated_at
            FROM t_commission_statement
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
              AND statement_month = #{statementMonth} AND coach_id = #{coachId}
            LIMIT 1
            """)
    MysqlCommissionPo.StatementPo findStatement(@Param("tenantId") Long tenantId,
                                                @Param("storeId") Long storeId,
                                                @Param("statementMonth") String statementMonth,
                                                @Param("coachId") Long coachId);

    @Insert("""
            INSERT INTO t_commission_statement(tenant_id, store_id, statement_no, statement_month, coach_id, rule_id,
                                               gross_amount, commission_amount, adjust_amount, final_amount, status,
                                               created_by, created_at, updated_at)
            VALUES(#{tenantId}, #{storeId}, #{statementNo}, #{statementMonth}, #{coachId}, #{ruleId},
                   #{grossAmount}, #{commissionAmount}, 0, #{commissionAmount}, #{status}, 0, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStatement(MysqlCommissionPo.StatementPo po);

    @Select("""
            SELECT id, tenant_id, store_id, statement_no, statement_month, coach_id, rule_id,
                   gross_amount, commission_amount, status, locked_at, created_at, updated_at
            FROM t_commission_statement
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    MysqlCommissionPo.StatementPo getStatement(@Param("id") Long id,
                                               @Param("tenantId") Long tenantId,
                                               @Param("storeId") Long storeId);

    @Update("""
            UPDATE t_commission_statement
            SET status = 'LOCKED', locked_at = #{lockedAt}, updated_at = #{lockedAt}
            WHERE id = #{id} AND tenant_id = #{tenantId} AND store_id = #{storeId}
            """)
    int lockStatement(@Param("id") Long id,
                      @Param("tenantId") Long tenantId,
                      @Param("storeId") Long storeId,
                      @Param("lockedAt") OffsetDateTime lockedAt);

    @Select("""
            SELECT id, tenant_id, store_id, statement_no, statement_month, coach_id, rule_id,
                   gross_amount, commission_amount, status, locked_at, created_at, updated_at
            FROM t_commission_statement
            WHERE tenant_id = #{tenantId} AND store_id = #{storeId}
              AND (#{statementMonth} IS NULL OR statement_month = #{statementMonth})
              AND (#{status} IS NULL OR status = #{status})
            ORDER BY id
            """)
    List<MysqlCommissionPo.StatementPo> listStatements(@Param("tenantId") Long tenantId,
                                                       @Param("storeId") Long storeId,
                                                       @Param("statementMonth") String statementMonth,
                                                       @Param("status") String status);
}
