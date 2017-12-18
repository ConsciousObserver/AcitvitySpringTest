package com.example;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ActivitiTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ActivitiTestApplication.class, args);
	}
	
	@Bean
	InitializingBean usersAndGroupsInitializer(final IdentityService identityService) {

	    return () -> {
	            Group group = identityService.newGroup("user");
	            group.setName("users");
	            group.setType("security-role");
	            identityService.saveGroup(group);

	            User admin = identityService.newUser("admin");
	            admin.setPassword("admin");
	            identityService.saveUser(admin);
	    	};
	}
}

@RestController
class ProcessRest {
	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	private TaskService taskService;
	
	@GetMapping("/start")
	public List<Map<String, String>> start() {
		List<String> photos = Arrays.asList("photo1", "photo2");
		runtimeService.startProcessInstanceByKey("simple-test-process", Collections.singletonMap("photos", photos));
		
		long runningProcessCount = runtimeService.createProcessInstanceQuery().count();
		List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery().list();
		
		return processes
				.stream()
				.map(p -> getMap(p.getId(), p.getProcessInstanceId(), p.getProcessDefinitionName()))
				.collect(Collectors.toList());
	}
	
	@GetMapping("/getTasks/{processInstanceId}")
	public List<Map<String, String>> getTasks(@PathVariable String processInstanceId) {
		List<Task> tasks = taskService
			.createTaskQuery()
			.processInstanceId(processInstanceId)
			.list();
		
		return tasks
				.stream()
				.map(t -> getMap(t.getId(), t.getProcessInstanceId(), t.getName()))
				.collect(Collectors.toList());
	}
	
	@GetMapping("/completeTask/{processInstanceId}")
	public Map<String, String> completeNextTask(@PathVariable String processInstanceId) {
		Task task = taskService.createTaskQuery()
						.processInstanceId(processInstanceId)
						.singleResult();
		
		if(task == null) {
			throw new RuntimeException("No pending task for this processInstance");
		}
		
		System.out.println("variables: " + task.getProcessVariables());
		
		taskService.complete(task.getId());
		
		return getMap(task.getId(), task.getProcessInstanceId(), task.getName());
	}
	
	public Map<String, String> getMap(String id, String instanceId, String name) {
		Map<String, String> map = new HashMap<>();
		map.put("id", id);
		map.put("processInstanceId", instanceId);
		map.put("name", name);
		
		return map;
	}
}
@Component
class MyService {
	public void init(List<String> photos) {
		System.out.println("################################### Running Init Task ########################");
		for(String photo: photos) {
			System.out.println("Photo name: " + photo);
		}
	}
	
	public String processPhoto(List<String> photos) {
		for(String photo: photos) {
			System.out.println("Photo name: " + photo);
		}
		return "processed";
	}
}

