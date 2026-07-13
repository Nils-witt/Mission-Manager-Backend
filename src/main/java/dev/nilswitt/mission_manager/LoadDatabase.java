package dev.nilswitt.mission_manager;


import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.records.DatabaseInitAdminUserRecord;
import dev.nilswitt.mission_manager.data.repositories.TenantRepository;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.data.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

@Configuration
class LoadDatabase {

    @Bean
    CommandLineRunner initDatabase(
            UserService repository,
            SecurityGroupService securityGroupRepository,
            TenantRepository tenantRepository,
            DatabaseInitAdminUserRecord adminUserRecord,
            PasswordEncoder passwordEncoder
    ) {
        // ToDo: Rebuild
        Optional<SecurityGroup> adminGroupOpt = securityGroupRepository.findByName("SuperAdmins");
        SecurityGroup adminGroup;

        Optional<Tenant> optDefaultTenant = tenantRepository.findByName("Default");
        Tenant defaultTenant;
        if (optDefaultTenant.isEmpty()) {
            defaultTenant = new Tenant("Default");
            tenantRepository.save(defaultTenant);
        } else {
            defaultTenant = optDefaultTenant.get();
        }


        if (adminGroupOpt.isEmpty()) {
            adminGroup = new SecurityGroup("SuperAdmins", new HashSet<>(SecurityGroup.availableRoles()));
        } else {
            adminGroup = adminGroupOpt.get();
            adminGroup.setRoles(new HashSet<>(SecurityGroup.availableRoles()));
        }
        adminGroup.setBuiltIn(true);
        securityGroupRepository.save(adminGroup);

        Optional<SecurityGroup> everyoneGroupOpt = securityGroupRepository.findByName("Everyone");
        SecurityGroup everyoneGroup;
        if (everyoneGroupOpt.isEmpty()) {
            everyoneGroup = new SecurityGroup("Everyone", new HashSet<>());
        } else {
            everyoneGroup = everyoneGroupOpt.get();
        }
        everyoneGroup.setBuiltIn(true);
        everyoneGroup = securityGroupRepository.save(everyoneGroup);

        if (adminUserRecord.create().equalsIgnoreCase("true")) {
            Optional<User> adminUserOpt = repository.findByUsername(adminUserRecord.username());
            if (adminUserOpt.isEmpty()) {
                User adminUser = new User(adminUserRecord.username(), "admin@admin.local", "Admin", "Admin");
                adminUser.setPassword(passwordEncoder.encode(adminUserRecord.password()));
                adminUser.addSecurityGroup(adminGroup);
                adminUser.addSecurityGroup(everyoneGroup);
                adminUser.setPrimaryTenant(defaultTenant);
                repository.save(adminUser);
            } else {
                if (adminUserRecord.force().equalsIgnoreCase("true")) {
                    User adminUser = adminUserOpt.get();
                    adminUser.setPassword(passwordEncoder.encode(adminUserRecord.password()));
                    adminUser.setLocked(false);
                    adminUser.setEnabled(true);
                    adminUser.addSecurityGroup(adminGroup);
                    adminUser.setPrimaryTenant(defaultTenant);
                    repository.save(adminUser);
                }
            }
        }

        return args -> {
        };
    }
}
