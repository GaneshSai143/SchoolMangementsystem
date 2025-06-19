package com.school.service.impl;

import com.school.dto.*; // UserDTO, TaskStatusDTO, TaskPriorityDTO
import com.school.entity.*;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException;
import com.school.repository.*;
import com.school.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    private User getCurrentUser(String username) {
        return userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    @Transactional
    public TaskDTO createTask(CreateTaskRequestDTO requestDTO, String authenticatedUsername) {
        User assigningUser = getCurrentUser(authenticatedUsername);
        Teacher teacherProfile = null;

        // Only teachers can create tasks for now, this logic might need adjustment based on roles
        if (assigningUser.getRole() == UserRole.TEACHER) {
            teacherProfile = teacherRepository.findByUserId(assigningUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + authenticatedUsername));
        } else {
            throw new UnauthorizedActionException("Only teachers can create tasks.");
        }

        Student student = null;
        if (requestDTO.getStudentId() != null) {
            student = studentRepository.findById(requestDTO.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + requestDTO.getStudentId()));
        }

        Classes taskClass = null;
        if (requestDTO.getClassId() != null) {
            taskClass = classRepository.findById(requestDTO.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + requestDTO.getClassId()));
        }

        if (student == null && taskClass == null) {
            throw new IllegalArgumentException("Task must be assigned to either a student or a class.");
        }

        // Further validation: if student is specified, ensure student belongs to teacher's school/class (complex)

        Task task = Task.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .dueDate(requestDTO.getDueDate())
                .status(modelMapper.map(requestDTO.getStatus(), com.school.entity.TaskStatus.class))
                .priority(requestDTO.getPriority() != null ? modelMapper.map(requestDTO.getPriority(), com.school.entity.TaskPriority.class) : null)
                .student(student) // Can be null if class task
                .classes(taskClass) // Can be null if student-specific task (though student implies class)
                .teacher(teacherProfile)
                .build();

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Override
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return convertToDTO(task);
    }

    @Override
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByStudentId(Long studentId) {
         if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return taskRepository.findByStudentId(studentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByTeacherId(Long teacherId) {
         if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher not found with id: " + teacherId);
        }
        return taskRepository.findByTeacherId(teacherId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByClassId(Long classId) {
         if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }
        return taskRepository.findByClassesId(classId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public TaskDTO updateTask(Long taskId, UpdateTaskRequestDTO requestDTO, String authenticatedUsername) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = getCurrentUser(authenticatedUsername);
        // Authorization: Only the teacher who assigned the task or an admin can update it.
        // Students might only update status (e.g. to COMPLETED).
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                 .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + authenticatedUsername));
            if (task.getTeacher() == null || !task.getTeacher().getId().equals(teacherProfile.getId())) {
                throw new UnauthorizedActionException("You are not authorized to update this task.");
            }
        } else if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SUPER_ADMIN) {
             // Student specific logic for status update could go here
             if (requestDTO.getStatus() != null && task.getStudent() != null && task.getStudent().getUser().getId().equals(currentUser.getId())) {
                 // Allow student to update only status
                 task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.TaskStatus.class));
                 Task updatedTask = taskRepository.save(task);
                 return convertToDTO(updatedTask);
             }
            throw new UnauthorizedActionException("You are not authorized to update this task details.");
        }


        if (requestDTO.getTitle() != null) task.setTitle(requestDTO.getTitle());
        if (requestDTO.getDescription() != null) task.setDescription(requestDTO.getDescription());
        if (requestDTO.getDueDate() != null) task.setDueDate(requestDTO.getDueDate());
        if (requestDTO.getStatus() != null) task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.TaskStatus.class));
        if (requestDTO.getPriority() != null) task.setPriority(modelMapper.map(requestDTO.getPriority(), com.school.entity.TaskPriority.class));

        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id, String authenticatedUsername) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = getCurrentUser(authenticatedUsername);
         // Authorization: Only the teacher who assigned the task or an admin can delete it.
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                 .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + authenticatedUsername));
            if (task.getTeacher() == null || !task.getTeacher().getId().equals(teacherProfile.getId())) {
                throw new UnauthorizedActionException("You are not authorized to delete this task.");
            }
        } else if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new UnauthorizedActionException("You are not authorized to delete this task.");
        }
        taskRepository.deleteById(id);
    }

    private TaskDTO convertToDTO(Task task) {
        TaskDTO taskDTO = modelMapper.map(task, TaskDTO.class);
        if (task.getStudent() != null) {
            taskDTO.setStudentId(task.getStudent().getId());
            if(task.getStudent().getUser() != null){
                taskDTO.setStudentName(task.getStudent().getUser().getFirstName() + " " + task.getStudent().getUser().getLastName());
            }
        }
        if (task.getTeacher() != null) {
            taskDTO.setTeacherId(task.getTeacher().getId());
             if(task.getTeacher().getUser() != null){
                taskDTO.setTeacherName(task.getTeacher().getUser().getFirstName() + " " + task.getTeacher().getUser().getLastName());
            }
        }
        if (task.getClasses() != null) {
            taskDTO.setClassId(task.getClasses().getId());
            taskDTO.setClassName(task.getClasses().getName());
        }
        // Map enums for status and priority
        if (task.getStatus() != null) {
            taskDTO.setStatus(modelMapper.map(task.getStatus(), TaskStatusDTO.class));
        }
        if (task.getPriority() != null) {
            taskDTO.setPriority(modelMapper.map(task.getPriority(), TaskPriorityDTO.class));
        }
        return taskDTO;
    }
}
