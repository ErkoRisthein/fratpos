package info.kaara.fratpos.security;

import info.kaara.fratpos.PosConfig;
import info.kaara.fratpos.security.model.Permission;
import info.kaara.fratpos.security.model.Role;
import info.kaara.fratpos.security.repository.PermissionRepository;
import info.kaara.fratpos.security.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.List;

import static info.kaara.fratpos.security.Permissions.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Creates some initial roles and permissions if there are none.
 */
@Slf4j
public class RolePermissionBootstrapListener implements ApplicationListener<ApplicationReadyEvent> {

	private static final String ROLES = "ROLES";

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		ConfigurableApplicationContext context = event.getApplicationContext();
		createAllPermissionsIfNotExists(context);
		createRolesRoleIfNotExists(context);
		createPosRoleIfNotExists(context);
	}

	private void createAllPermissionsIfNotExists(ConfigurableApplicationContext context) {
		PermissionRepository permissionRepository = context.getBean(PermissionRepository.class);
		if (permissionRepository.count() > 0) {
			return;
		}
		log.info("Permissions don't exist, creating");
		Arrays.stream(Permissions.values())
				.map(Permissions::name)
				.map(Permission::new)
				.map(permissionRepository::save)
				.collect(toList());
	}

	private void createRolesRoleIfNotExists(ConfigurableApplicationContext context) {
		createRoleIfNotExists(context, ROLES, ROLES_MODIFY, ROLES_VIEW);
	}

	private void createPosRoleIfNotExists(ConfigurableApplicationContext context) {
		String posRole = getPosRole(context);
		createRoleIfNotExists(context, posRole, USERS_VIEW, USERS_MODIFY, POS_VIEW, POS_MODIFY);
	}

	private void createRoleIfNotExists(ConfigurableApplicationContext context, String role, Permissions... permissions) {
		RoleRepository roleRepository = context.getBean(RoleRepository.class);
		if (roleRepository.findOneByName(role) != null) {
			return;
		}
		log.info("Role {} doesn't exist, creating", role);
		Role r = new Role();
		r.setName(role);
		r.setPermissions(getPermissions(context, permissions));
		roleRepository.save(r);
	}

	private List<Permission> getPermissions(ConfigurableApplicationContext context, Permissions... permissions) {
		PermissionRepository permissionRepository = context.getBean(PermissionRepository.class);
		List<String> permissionNames = asList(permissions).stream()
				.map(Permissions::name).collect(toList());
		return permissionRepository.findByNameIn(permissionNames);
	}

	private String getPosRole(ConfigurableApplicationContext context) {
		PosConfig posConfig = context.getBean(PosConfig.class);
		return posConfig.getRole();
	}
}
