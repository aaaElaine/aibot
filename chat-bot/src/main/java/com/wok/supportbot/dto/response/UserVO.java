package com.wok.supportbot.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户视图对象（扩展：机构信息、角色、权限）
 */
@Data
public class UserVO {

    private Long id;

    /** 归属机构ID：0=平台账号 */
    private Long orgId;

    /** 归属机构名 */
    private String orgName;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    /** 单字段角色（兼容老字段） */
    private String role;

    private String status;

    private LocalDateTime lastLoginTime;

    /** 关联的角色编码列表 */
    private Set<String> roleCodes;

    /** 关联的角色名列表 */
    private Set<String> roleNames;

    /** 关联的角色ID列表 */
    private List<Long> roleIds;

    /** 是否平台超管 */
    private Boolean platformAdmin;

    /** 是否机构管理员 */
    private Boolean orgAdmin;

    /** 权限编码集合 */
    private Set<String> permissionCodes;
}
