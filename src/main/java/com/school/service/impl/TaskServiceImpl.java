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
    private final UserRepository userRepository; // Kept for now, though currentUser is passed directly
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final SubjectAssignmentServiceImpl subjectAssignmentServiceImpl; // Added for convertToDTO
    private final ModelMapper modelMapper;

    // This method might be removed if currentUser is always passed in.
    // private User getCurrentUser(String username) {
    //     return userRepository.findByEmail(username)
    //         .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    // }

    @Override
    @Transactional
    public TaskDTO createTask(CreateTaskRequestDTO requestDTO, User currentUser) {
        // User assigningUser = getCurrentUser(authenticatedUsername); // Now currentUser is passed
        Teacher teacherProfile = null;

        // Only teachers can create tasks for now
        if (currentUser.getRole() == UserRole.TEACHER) {
            teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
        } else {
            throw new UnauthorizedActionException("Only teachers can create tasks.");
        }

        SubjectAssignment subjectAssignment = null;
        if (requestDTO.getSubjectAssignmentId() != null) {
            subjectAssignment = subjectAssignmentRepository.findById(requestDTO.getSubjectAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject Assignment not found with id: " + requestDTO.getSubjectAssignmentId()));
        }

        Student student = null;
        if (requestDTO.getStudentId() != null) {
            student = studentRepository.findById(requestDTO.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + requestDTO.getStudentId()));
        }

        Classes requestDtoClass = null; // Class from requestDTO.getClassId()
        if (requestDTO.getClassId() != null) {
            requestDtoClass = classRepository.findById(requestDTO.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + requestDTO.getClassId()));
        }

        Classes determinedClass = null; // Variable to hold the final class for the task

        if (subjectAssignment != null) {
            determinedClass = subjectAssignment.getClasses();
            if (requestDtoClass != null && !requestDtoClass.getId().equals(determinedClass.getId())) {
                throw new IllegalArgumentException("Provided classId (" + requestDTO.getClassId() + ") does not match the class in SubjectAssignment (" + determinedClass.getId() + ").");
            }
            if (student != null && !student.getClasses().getId().equals(determinedClass.getId())) {
                throw new IllegalArgumentException("Student's class does not match the class in the SubjectAssignment.");
            }
             // Ensure the assigning teacher is the one in the subject assignment
            if (teacherProfile != null && !subjectAssignment.getTeacher().getId().equals(teacherProfile.getId())) {
                 throw new UnauthorizedActionException("Authenticated teacher is not the assigned teacher for the specified Subject Assignment.");
            }
        } else if (requestDtoClass != null) { // taskClass from requestDTO.getClassId(), and no SubjectAssignment
            determinedClass = requestDtoClass;
            if (student != null && !student.getClasses().getId().equals(determinedClass.getId())) {
                throw new IllegalArgumentException("Student's class does not match the provided classId.");
            }
        } else if (student != null) { // No SubjectAssignment, no requestDTO.classId, but student is present
            determinedClass = student.getClasses();
            if (determinedClass == null) {
                throw new IllegalArgumentException("Student (" + student.getId() + ") is not associated with any class. Task cannot be created without a class context.");
            }
        } else {
            throw new IllegalArgumentException("Task cannot be created without a student, class, or subject assignment context.");
        }

        if (determinedClass == null) {
            throw new IllegalStateException("Failed to determine class context for the task.");
        }

        Task task = Task.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .dueDate(requestDTO.getDueDate())
                .status(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class))
                .priority(requestDTO.getPriority() != null ? modelMapper.map(requestDTO.getPriority(), com.school.entity.enums.TaskPriority.class) : null)
                .student(student)
                .classes(determinedClass)
                .teacher(teacherProfile)
                .subjectAssignment(subjectAssignment)
                .build();

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Override
    public TaskDTO getTaskById(Long id, User currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            boolean canAccess = false;
            if (task.getTeacher() != null && task.getTeacher().getId().equals(teacherProfile.getId())) {
                canAccess = true; // Teacher assigned this task
            } else if (task.getStudent() != null && task.getStudent().getClasses() != null &&
                       subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(
                           task.getStudent().getClasses().getId(), teacherProfile.getId(), "ACTIVE")) {
                canAccess = true; // Teacher teaches the student's class
            } else if (task.getClasses() != null &&
                       subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(
                           task.getClasses().getId(), teacherProfile.getId(), "ACTIVE")) {
                canAccess = true; // Teacher teaches the class this task is for
            }
            if (!canAccess) {
                throw new UnauthorizedActionException("Teacher is not authorized to view this task.");
            }
        } else if (currentUser.getRole() == UserRole.STUDENT) {
            Student studentProfile = studentRepository.findByUserId(currentUser.getId())
                     .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + currentUser.getEmail()));
            if (task.getStudent() == null || !task.getStudent().getId().equals(studentProfile.getId())){
                 throw new UnauthorizedActionException("Student is not authorized to view this task.");
            }
        }
        // ADMIN/SUPER_ADMIN can view all tasks.
        return convertToDTO(task);
    }

    @Override
    public List<TaskDTO> getAllTasks(User currentUser) { // For admin overview
        if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new UnauthorizedActionException("Only Admins or Super Admins can view all tasks.");
        }
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByStudentId(Long studentId, User currentUser) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (student.getClasses() == null || !subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(student.getClasses().getId(), teacherProfile.getId(), "ACTIVE")) {
                throw new UnauthorizedActionException("Teacher is not actively assigned to this student's class.");
            }
        } else if (currentUser.getRole() == UserRole.STUDENT) {
            if (!student.getUser().getId().equals(currentUser.getId())) {
                 throw new UnauthorizedActionException("Students can only view their own tasks.");
            }
        }
        // ADMIN/SUPER_ADMIN can view.
        return taskRepository.findByStudentId(studentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByTeacherId(Long teacherId, User currentUser) {
        Teacher targetTeacher = teacherRepository.findById(teacherId)
             .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        if (currentUser.getRole() == UserRole.TEACHER) {
            if (!targetTeacher.getUser().getId().equals(currentUser.getId())) {
                 throw new UnauthorizedActionException("Teachers can only view their own assigned tasks.");
            }
        }
        // ADMIN/SUPER_ADMIN can view.
        return taskRepository.findByTeacherId(teacherId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByClassId(Long classId, User currentUser) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found with id: " + classId);
        }
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (!subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(classId, teacherProfile.getId(), "ACTIVE")) {
                throw new UnauthorizedActionException("Teacher is not actively assigned to this class's tasks.");
            }
        }
        // ADMIN/SUPER_ADMIN can view. Students in class might be another rule.
        return taskRepository.findByClassesId(classId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public TaskDTO updateTask(Long taskId, UpdateTaskRequestDTO requestDTO, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // User currentUser = getCurrentUser(authenticatedUsername); // Now currentUser is passed
        // Authorization
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                 .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
            if (task.getTeacher() == null || !task.getTeacher().getId().equals(teacherProfile.getId())) {
                throw new UnauthorizedActionException("You are not authorized to update this task.");
            }
            // Teacher can update all fields they set
            if (requestDTO.getTitle() != null) task.setTitle(requestDTO.getTitle());
            if (requestDTO.getDescription() != null) task.setDescription(requestDTO.getDescription());
            if (requestDTO.getDueDate() != null) task.setDueDate(requestDTO.getDueDate());
            if (requestDTO.getPriority() != null) task.setPriority(modelMapper.map(requestDTO.getPriority(), com.school.entity.enums.TaskPriority.class));
            // Teacher can change status, unless it's a student-specific status update flow not covered here
            if (requestDTO.getStatus() != null) task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class));

        } else if (currentUser.getRole() == UserRole.STUDENT) {
             if (task.getStudent() == null || !task.getStudent().getUser().getId().equals(currentUser.getId())) {
                throw new UnauthorizedActionException("You are not authorized to update this task.");
             }
             // Student can only update status to certain values, e.g., COMPLETED
             if (requestDTO.getStatus() != null ) { // Simplified: student can update status
                 task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class));
             } else if (requestDTO.getTitle() != null || requestDTO.getDescription() != null || requestDTO.getDueDate() != null || requestDTO.getPriority() != null) {
                 throw new UnauthorizedActionException("Students can only update the status of their tasks.");
             }
        } else if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN) {
            // Admin/SuperAdmin can update all fields
            if (requestDTO.getTitle() != null) task.setTitle(requestDTO.getTitle());
            if (requestDTO.getDescription() != null) task.setDescription(requestDTO.getDescription());
            if (requestDTO.getDueDate() != null) task.setDueDate(requestDTO.getDueDate());
            if (requestDTO.getStatus() != null) task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class));
            if (requestDTO.getPriority() != null) task.setPriority(modelMapper.map(requestDTO.getPriority(), com.school.entity.enums.TaskPriority.class));
        } else {
             throw new UnauthorizedActionException("You are not authorized to update this task details.");
        }

        /* Original logic:
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
                 task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class));
                 Task updatedTask = taskRepository.save(task);
                 return convertToDTO(updatedTask);
             }
            // throw new UnauthorizedActionException("You are not authorized to update this task details."); // Covered by else
        }
        /* Original logic:
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
                 task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class));
                 Task updatedTask = taskRepository.save(task);
                 return convertToDTO(updatedTask);
             }
             throw new UnauthorizedActionException("You are not authorized to update this task details.");
         }


-        if (requestDTO.getTitle() != null) task.setTitle(requestDTO.getTitle());
-        if (requestDTO.getDescription() != null) task.setDescription(requestDTO.getDescription());
-        if (requestDTO.getDueDate() != null) task.setDueDate(requestDTO.getDueDate());
-        if (requestDTO.getStatus() != null) task.setStatus(modelMapper.map(requestDTO.getStatus(), com.school.entity.enums.TaskStatus.class));
-        if (requestDTO.getPriority() != null) task.setPriority(modelMapper.map(requestDTO.getPriority(), com.school.entity.enums.TaskPriority.class));
-
+        */
        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id, User currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // User currentUser = getCurrentUser(authenticatedUsername); // Now currentUser is passed
         // Authorization: Only the teacher who assigned the task or an admin can delete it.
        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher teacherProfile = teacherRepository.findByUserId(currentUser.getId())
                 .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));
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
        if (task.getTeacher() != null) { // This is the creator of the task
            taskDTO.setTeacherId(task.getTeacher().getId());
             if(task.getTeacher().getUser() != null){
                taskDTO.setTeacherName(task.getTeacher().getUser().getFirstName() + " " + task.getTeacher().getUser().getLastName());
            }
        }
        if (task.getClasses() != null) {
            taskDTO.setClassId(task.getClasses().getId());
            taskDTO.setClassName(task.getClasses().getName());
        }
        if (task.getSubjectAssignment() != null) {
            taskDTO.setSubjectAssignment(subjectAssignmentServiceImpl.convertToResponseDTO(task.getSubjectAssignment()));
        } else {
            taskDTO.setSubjectAssignment(null);
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
