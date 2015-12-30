package info.kaara.fratpos.user.controller;

import info.kaara.fratpos.common.controller.RestBaseController;
import info.kaara.fratpos.is.model.Obligation;
import info.kaara.fratpos.is.model.RecurringUserObligation;
import info.kaara.fratpos.is.model.UserObligation;
import info.kaara.fratpos.is.repository.ObligationRepository;
import info.kaara.fratpos.is.repository.UserObligationRepository;
import info.kaara.fratpos.security.model.Role;
import info.kaara.fratpos.security.repository.RoleRepository;
import info.kaara.fratpos.user.model.User;
import info.kaara.fratpos.user.model.UserProfile;
import info.kaara.fratpos.user.repository.UserProfileRepository;
import info.kaara.fratpos.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping(value = "/user")
public class UserController extends RestBaseController<User, Long> {

	@Autowired
	private RoleRepository roleRepository;

	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private UserObligationRepository userObligationRepository;

	@Autowired
	private ObligationRepository obligationRepository;

	private final Object UPDATE_LOCK = new Object();

	@Autowired
	public UserController(UserRepository userRepository) {
		super(userRepository, "ROLE_USERS");
		this.userRepository = userRepository;
	}

	@RequestMapping(value = "/me", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<User> get(SecurityContextHolderAwareRequestWrapper request) {
		if (canRead(request)) {
			String email = request.getRemoteUser();
			User me = userRepository.findByEmail(email);
			if (me != null) {
				return new ResponseEntity<>(me, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User json, SecurityContextHolderAwareRequestWrapper request) {
		log.info("update() of id {} with body {}", id, json);
		log.info("T json is of type {}", json.getClass());
		if (canModify(request)) {
			User entity = repo.findOne(id);
			try {
				BeanUtils.copyProperties(json, entity, "password", "roles", "userProfile");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			User updated = repo.save(entity);
			log.info("updated entity: {}", updated);
			return new ResponseEntity<>(updated, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}/role/{roleId}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<User> addRole(@PathVariable("roleId") Long roleId, @PathVariable("id") Long id, SecurityContextHolderAwareRequestWrapper request) {
		if (canModify(request)) {
			synchronized (UPDATE_LOCK) {
				User user = repo.findOne(id);
				Role role = roleRepository.findOne(roleId);
				if (!user.getRoles().contains(role)) {
					log.info("Adding role {} to user {}", role.getName(), user.getLabel());
					user.getRoles().add(role);
					repo.save(user);
				}
			}
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}/role/{roleId}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<Role> removeRole(@PathVariable("roleId") Long roleId, @PathVariable("id") Long id, SecurityContextHolderAwareRequestWrapper request) {
		if (canModify(request)) {
			synchronized (UPDATE_LOCK) {
				User user = repo.findOne(id);
				Role role = roleRepository.findOne(roleId);
				if (user.getRoles().contains(role)) {
					log.info("Removing role {} from user {}", role.getName(), user.getLabel());
					user.getRoles().remove(role);
					repo.save(user);
				}
			}
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}/userprofile", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public ResponseEntity<UserProfile> createProfile(@RequestBody UserProfile json, @PathVariable("id") Long id, SecurityContextHolderAwareRequestWrapper request) {
		log.info("create() with body {} of type {}", json, json.getClass());
		if (canModify(request)) {
			User user = repo.findOne(id);
			if (user.getUserProfile() != null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			json.setUser(user);
			UserProfile userProfile = userProfileRepository.save(json);
			return new ResponseEntity<>(userProfile, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}/userprofile/{userProfileId}", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public ResponseEntity<UserProfile> update(@PathVariable("userProfileId") Long userProfileId, @PathVariable("id") Long id, @RequestBody UserProfile json, SecurityContextHolderAwareRequestWrapper request) {
		log.info("update() of id {} with body {}", id, json);
		log.info("T json is of type {}", json.getClass());
		if (canModify(request)) {
			UserProfile entity = userProfileRepository.findOne(userProfileId);
			try {
				BeanUtils.copyProperties(json, entity, "user");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			UserProfile updated = userProfileRepository.save(entity);
			log.info("updated entity: {}", updated);
			return new ResponseEntity<>(updated, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}/obligation/{obligationId}", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public ResponseEntity<UserObligation> createObligation(@RequestBody UserObligation json, @PathVariable("id") Long id, @PathVariable("obligationId") Long obligationId, SecurityContextHolderAwareRequestWrapper request) {
		log.info("create() with body {} of type {}", json, json.getClass());
		if (canModify(request)) {
			User user = repo.findOne(id);
			Obligation obligation = obligationRepository.findOne(obligationId);
			json.setUser(user);
			json.setObligation(obligation);
			UserObligation userObligation = userObligationRepository.save(json);
			return new ResponseEntity<>(userObligation, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/{id}/obligation/{obligationId}/recurring", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public ResponseEntity<RecurringUserObligation> createRecurringObligation(@RequestBody RecurringUserObligation json, @PathVariable("id") Long id, @PathVariable("obligationId") Long obligationId, SecurityContextHolderAwareRequestWrapper request) {
		log.info("create() with body {} of type {}", json, json.getClass());
		if (canModify(request)) {
			User user = repo.findOne(id);
			Obligation obligation = obligationRepository.findOne(obligationId);
			json.setUser(user);
			json.setObligation(obligation);
			RecurringUserObligation userObligation = userObligationRepository.save(json);
			return new ResponseEntity<>(userObligation, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}
}
