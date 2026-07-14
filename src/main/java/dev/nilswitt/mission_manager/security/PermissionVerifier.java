package dev.nilswitt.mission_manager.security;


import dev.nilswitt.mission_manager.data.entities.*;
import dev.nilswitt.mission_manager.data.repositories.UserRepository;
import dev.nilswitt.mission_manager.data.services.SecurityGroupPermissionService;
import dev.nilswitt.mission_manager.data.services.UserPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public final class PermissionVerifier {

    private final UserPermissionService userPermissionService;
    private final SecurityGroupPermissionService securityGroupPermissionService;
    private final UserRepository userRepository;


    private PermissionVerifier(
            UserPermissionService userPermissionService,
            SecurityGroupPermissionService securityGroupPermissionService,
            UserRepository userRepository
    ) {
        this.userPermissionService = userPermissionService;
        this.securityGroupPermissionService = securityGroupPermissionService;
        this.userRepository = userRepository;
    }

    public static boolean testScope(
            SecurityGroup.UserRoleScopeEnum requiredScope,
            SecurityGroup.UserRoleScopeEnum providedScope
    ) {
        return switch (requiredScope) {
            case VIEW -> isView(providedScope);
            case EDIT -> isEdit(providedScope);
            case CREATE -> isCreate(providedScope);
            case DELETE -> isDelete(providedScope);
            case ADMIN -> providedScope == SecurityGroup.UserRoleScopeEnum.ADMIN;
        };
    }

    public static boolean isView(SecurityGroup.UserRoleScopeEnum toTest) {
        return (
                toTest == SecurityGroup.UserRoleScopeEnum.VIEW ||
                        toTest == SecurityGroup.UserRoleScopeEnum.EDIT ||
                        toTest == SecurityGroup.UserRoleScopeEnum.ADMIN
        );
    }

    public static boolean isEdit(SecurityGroup.UserRoleScopeEnum toTest) {
        return (
                toTest == SecurityGroup.UserRoleScopeEnum.VIEW ||
                        toTest == SecurityGroup.UserRoleScopeEnum.EDIT ||
                        toTest == SecurityGroup.UserRoleScopeEnum.ADMIN
        );
    }

    public static boolean isCreate(SecurityGroup.UserRoleScopeEnum toTest) {
        return (toTest == SecurityGroup.UserRoleScopeEnum.CREATE || toTest == SecurityGroup.UserRoleScopeEnum.ADMIN);
    }

    public static boolean isDelete(SecurityGroup.UserRoleScopeEnum toTest) {
        return (toTest == SecurityGroup.UserRoleScopeEnum.DELETE || toTest == SecurityGroup.UserRoleScopeEnum.ADMIN);
    }

    public static boolean hasAnyScope(
            User user,
            SecurityGroup.UserRoleTypeEnum type,
            SecurityGroup.UserRoleScopeEnum... scopes
    ) {
        if (scopes == null || scopes.length == 0) {
            return false;
        }
        return Arrays.stream(scopes).anyMatch(scope -> hasScope(user, type, scope));
    }

    private static boolean hasScope(
            User user,
            SecurityGroup.UserRoleTypeEnum type,
            SecurityGroup.UserRoleScopeEnum scope
    ) {
        if (user == null || type == null || scope == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        String requiredRole = buildRole(type, scope);
        String typeAdminRole = buildRole(type, SecurityGroup.UserRoleScopeEnum.ADMIN);
        String globalScopeRole = buildRole(SecurityGroup.UserRoleTypeEnum.GLOBAL, scope);
        String globalAdminRole = buildRole(
                SecurityGroup.UserRoleTypeEnum.GLOBAL,
                SecurityGroup.UserRoleScopeEnum.ADMIN
        );

        return authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(
                        role ->
                                role.equals(requiredRole) ||
                                        role.equals(typeAdminRole) ||
                                        role.equals(globalScopeRole) ||
                                        role.equals(globalAdminRole)
                );
    }

    private static String buildRole(SecurityGroup.UserRoleTypeEnum type, SecurityGroup.UserRoleScopeEnum scope) {
        return "ROLE_" + type.name() + "_" + scope.name();
    }

    public boolean hasAccess(
            User user,
            SecurityGroup.UserRoleScopeEnum requiredScope,
            SecurityGroup.UserRoleTypeEnum type
    ) {
        String requiredRole = buildRole(type, requiredScope);
        String typeAdminRole = buildRole(type, SecurityGroup.UserRoleScopeEnum.ADMIN);
        String globalScopeRole = buildRole(SecurityGroup.UserRoleTypeEnum.GLOBAL, requiredScope);
        String globalAdminRole = buildRole(
                SecurityGroup.UserRoleTypeEnum.GLOBAL,
                SecurityGroup.UserRoleScopeEnum.ADMIN
        );

        return user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(
                        role ->
                                role.equals(requiredRole) ||
                                        role.equals(typeAdminRole) ||
                                        role.equals(globalScopeRole) ||
                                        role.equals(globalAdminRole)
                );
    }

    public boolean hasAccess(User user, SecurityGroup.UserRoleScopeEnum requiredScope, Mission mission) {
        if (hasScope(user, SecurityGroup.UserRoleTypeEnum.MISSION, requiredScope)) {
            return true;
        }

        return getScopes(mission, user).contains(requiredScope);
    }


    public boolean hasAccess(User user, SecurityGroup.UserRoleScopeEnum requiredScope, User checkUser) {
        if (hasAnyScope(user, SecurityGroup.UserRoleTypeEnum.USER, SecurityGroup.UserRoleScopeEnum.ADMIN)) {
            return true;
        }

        return getScopes(checkUser, user).contains(requiredScope);
    }


    public List<Mission> getMissionsForUser(User userDetails) {
        ArrayList<Mission> permittedItems = new ArrayList<>(
                this.userPermissionService.findByUserAndMissionNotNull(userDetails)
                        .stream()
                        .map(UserPermission::getMission)
                        .toList()
        );
        for (SecurityGroup sg : userDetails.getSecurityGroups()) {
            permittedItems.addAll(
                    this.securityGroupPermissionService.findBySecurityGroupAndMissionNotNull(sg)
                            .stream()
                            .map(SecurityGroupPermission::getMission)
                            .toList()
            );
        }
        return permittedItems.stream().distinct().toList();
    }


    public List<User> getUsersForUser(User userDetails) {
        ArrayList<User> permittedOverlays = new ArrayList<>(
                this.userPermissionService.findByUserAndEntityUserNotNull(userDetails)
                        .stream()
                        .map(UserPermission::getEntityUser)
                        .toList()
        );
        for (SecurityGroup sg : userDetails.getSecurityGroups()) {
            permittedOverlays.addAll(
                    this.securityGroupPermissionService.findBySecurityGroupAndEntityUserNotNull(sg)
                            .stream()
                            .map(SecurityGroupPermission::getEntityUser)
                            .toList()
            );
        }
        permittedOverlays.add(userDetails);
        return permittedOverlays.stream().distinct().toList();
    }

    public Set<SecurityGroup.UserRoleScopeEnum> getScopes(AbstractEntity entity, User user) {
        return switch (entity) {
            case Mission mission -> getScopes(mission, user);
            case User user1 -> getScopes(user1, user);

            case null, default -> Set.of();
        };
    }

    /**
     * Blanket, type-level scopes only - for entity types that have no per-instance permission
     * overrides (i.e. everything except {@link Mission} and {@link User}).
     */
    public Set<SecurityGroup.UserRoleScopeEnum> getScopes(SecurityGroup.UserRoleTypeEnum type, User user) {
        HashSet<SecurityGroup.UserRoleScopeEnum> scopes = new HashSet<>();

        for (SecurityGroup.UserRoleScopeEnum scope : SecurityGroup.UserRoleScopeEnum.values()) {
            if (hasScope(user, type, scope)) {
                scopes.add(scope);
            }
        }

        return scopes;
    }

    public Set<SecurityGroup.UserRoleScopeEnum> getScopes(Mission mission, User user) {
        HashSet<SecurityGroup.UserRoleScopeEnum> scopes = new HashSet<>();

        for (SecurityGroup.UserRoleScopeEnum scope : SecurityGroup.UserRoleScopeEnum.values()) {
            if (hasScope(user, SecurityGroup.UserRoleTypeEnum.MISSION, scope)) {
                scopes.add(scope);
            }
        }

        if (isMemberOfTenant(user, mission.getTenant())) {
            scopes.add(SecurityGroup.UserRoleScopeEnum.VIEW);
        }

        this.userPermissionService.findByUserAndMission(user, mission).ifPresent(perm -> scopes.add(perm.getScope()));
        for (SecurityGroup sg : user.getSecurityGroups()) {
            this.securityGroupPermissionService.findBySecurityGroupAndMission(sg, mission)
                    .stream()
                    .map(SecurityGroupPermission::getScope)
                    .forEach(scopes::add);
        }

        return scopes;
    }

    /**
     * Looked up via a repository query rather than {@code user.getTenants()} because the
     * {@code User} behind {@code @AuthenticationPrincipal} is detached and its lazy tenants
     * collection cannot be initialized outside the session it was originally loaded in.
     */
    private boolean isMemberOfTenant(User user, Tenant tenant) {
        if (tenant == null || user == null || user.getId() == null) {
            return false;
        }
        return userRepository.existsByIdAndTenants_Id(user.getId(), tenant.getId());
    }

    public Set<SecurityGroup.UserRoleScopeEnum> getScopes(User target, User user) {
        HashSet<SecurityGroup.UserRoleScopeEnum> scopes = new HashSet<>();

        // direct permissions
        for (SecurityGroup.UserRoleScopeEnum scope : SecurityGroup.UserRoleScopeEnum.values()) {
            if (hasScope(user, SecurityGroup.UserRoleTypeEnum.MISSIONGROUP, scope)) {
                scopes.add(scope);
            }
        }

        this.userPermissionService.findByUserAndEntityUser(user, target).ifPresent(perm -> scopes.add(perm.getScope()));
        for (SecurityGroup sg : user.getSecurityGroups()) {
            this.securityGroupPermissionService.findBySecurityGroupAndEntityUser(sg, target)
                    .stream()
                    .map(SecurityGroupPermission::getScope)
                    .forEach(scopes::add);
        }

        // -- Custom addition based on relations

        return scopes;
    }

}
