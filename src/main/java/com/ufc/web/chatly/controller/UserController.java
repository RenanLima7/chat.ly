package com.ufc.web.chatly.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufc.web.chatly.common.BaseController;
import com.ufc.web.chatly.common.BaseMessage;
import com.ufc.web.chatly.common.UtilityMethods;
import com.ufc.web.chatly.dto.ContactDTO;
import com.ufc.web.chatly.dto.UserDTO;
import com.ufc.web.chatly.model.Contact;
import com.ufc.web.chatly.model.User;
import com.ufc.web.chatly.service.UserService;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/user")
public class UserController implements BaseController<User, UserDTO>{
	@Autowired 
	UserService userService;	
	
	@GetMapping
	public ResponseEntity<Iterable<User>> getAll() {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getAll());
	}

	@Transactional
	@PostMapping
	@Override
	public ResponseEntity<Object> save(@RequestBody @Valid UserDTO userDTO) {
		var user = new User();
		BeanUtils.copyProperties(userDTO, user);
		
		Optional<User> userOptional = userService.getByEmail(userDTO.getEmail());
		
		if (userOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("This email alread exists: " + userDTO.getEmail()), HttpStatus.CONFLICT);
		}
		
		if (!UtilityMethods.isNull(userDTO.getAvatar()) && !userDTO.getAvatar().isEmpty()) {
			byte[] avatar = UtilityMethods.base64Decode(userDTO.getAvatar()).getBytes();			
			user.setAvatar(avatar);			
		} else {
			user.setAvatar(null);
		}
			
		if (!UtilityMethods.isNull(userDTO.getPassword()) && !userDTO.getPassword().isEmpty()) {
			String hash = UtilityMethods.encryptWithBCrypt(userDTO.getPassword());			
			user.setPassword(hash);			
		}
		
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.save(user));		
	}

	@Transactional
	@PutMapping("/{id}")
	@Override
	public ResponseEntity<Object> update(@RequestBody @Valid UserDTO userDTO, @PathVariable(value = "id") Long id) {
		Optional<User> userOptional = userService.getById(id);	
		
		if (!userOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("No user found with id: " + id), HttpStatus.NOT_FOUND);
		}
		
		User user = userOptional.get();
		user.setName(userDTO.getName());
		user.setGenre(userDTO.getGenre());

		if (!UtilityMethods.isNull(userDTO.getPassword()) && !userDTO.getPassword().isEmpty()) {
			String hash = UtilityMethods.encryptWithBCrypt(userDTO.getPassword());			
			user.setPassword(hash);			
		}
		
		if(user.getEmail().equals(userDTO.getEmail())) {
			user.setEmail(userDTO.getEmail());			
		} else {
			userOptional = userService.getByEmail(userDTO.getEmail());
			
			if (userOptional.isPresent()) {
				return new ResponseEntity<Object>(new BaseMessage("This email alread exists: " + userDTO.getEmail()), HttpStatus.CONFLICT);
			}
			
			user.setEmail(userDTO.getEmail());	
		}
				
		if (!UtilityMethods.isNull(userDTO.getAvatar()) && !userDTO.getAvatar().isEmpty()) {
			byte[] avatar = UtilityMethods.base64Decode(userDTO.getAvatar()).getBytes();			
			user.setAvatar(avatar);			
		} else {
			user.setAvatar(null);
		}		
		
		userService.save(user);
		
		return ResponseEntity.status(HttpStatus.OK).body(userService.save(user));
	}

	@Transactional
	@DeleteMapping("/{id}")
	@Override
	public ResponseEntity<Object> delete(@PathVariable(value = "id") Long id) {
		Optional<User> userOptional = userService.getById(id);
		
		if (!userOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("No user found with id: " + id), HttpStatus.NOT_FOUND);
		}
		
		userService.delete(userOptional.get());
		return new ResponseEntity<Object>(new BaseMessage("User successfully deleted!"), HttpStatus.OK);	
	}

	@GetMapping("/{id}")
	@Override
	public ResponseEntity<Object> getById(@PathVariable(value = "id") Long id) {
		Optional<User> userOptional = userService.getById(id);
		
		if (!userOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("No user found with id: " + id), HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.status(HttpStatus.OK).body(userOptional.get());
	}

	@PostMapping("/login")
	public ResponseEntity<Object> userLogin(@RequestHeader(value="Authorization") String basicAuth) {
					
		String auth = UtilityMethods.base64Decode(basicAuth.substring(6, basicAuth.length()));
		String[] authSplited = auth.split(":");
		String email = authSplited[0];
		String password = authSplited[1];
		
		Optional<User> userOptional = userService.getByEmail(email);
		
		if (!userOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("There is no user with the email: " + email), HttpStatus.UNAUTHORIZED);
		}
		
		User user = userOptional.get();
		boolean valid = UtilityMethods.compareEncryptedPassword(password, user.getPassword());
		
		if(valid) {
			return new ResponseEntity<Object>(new BaseMessage("AUTHORIZED"), HttpStatus.ACCEPTED);
		} else {
			return new ResponseEntity<Object>(new BaseMessage("UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
		}
	}

	@Transactional
	@PutMapping("/addContact")
	public ResponseEntity<Object> addContact(@RequestBody @Valid ContactDTO contactDTO){
		
		Optional<User> userOptional = userService.getById(contactDTO.getUserId());	
		Optional<User> contactOptional = userService.getById(contactDTO.getContactId());
		
		if (!userOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("No user found with id: " + contactDTO.getUserId()), HttpStatus.NOT_FOUND);
		}		
		
		if (!contactOptional.isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("No user found with id: " + contactDTO.getContactId()), HttpStatus.NOT_FOUND);
		}
		
		if (contactDTO.getUserId() == contactDTO.getContactId()) {
			return new ResponseEntity<Object>(new BaseMessage("You can't add yourself"), HttpStatus.CONFLICT);
		}
		
		/*if (userService.findByUserIdAndContactId(contactDTO.getUserId(), contactDTO.getContactId()).isPresent()) {
			return new ResponseEntity<Object>(new BaseMessage("Contact is already added"), HttpStatus.CONFLICT);
		}*/
		
		User user = userOptional.get();
		Contact contact = new Contact();
		
		contact.setName(contactOptional.get().getName());
		contact.setEmail(contactOptional.get().getEmail());
		contact.setGenre(contactOptional.get().getGenre());
		contact.setAvatar(contactOptional.get().getAvatar());
		
		user.setContacts(Arrays.asList(contact));		
		userService.save(user);
		
		return ResponseEntity.status(HttpStatus.OK).body("Contact successfully added!");	
	}

	@GetMapping("/source/{source}")
	public ResponseEntity<Object> getBySource(@PathVariable(value = "source") String source) {	
		
		ArrayList<User> users = new ArrayList<User>();
		users.addAll((Collection<? extends User>) userService.findByEmail(source));
		users.addAll((Collection<? extends User>) userService.findBySource(source));
		
		return ResponseEntity.status(HttpStatus.OK).body(users);
	}
}