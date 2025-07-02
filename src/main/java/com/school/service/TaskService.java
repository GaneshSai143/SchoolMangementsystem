package com.school.service;

import com.school.dto.CreateTaskRequestDTO;
import com.school.dto.TaskDTO;
import com.school.dto.UpdateTaskRequestDTO;
import com.school.entity.User; // Added
import java.util.List;

public interface TaskService {
    TaskDTO createTask(CreateTaskRequestDTO requestDTO, User currentUser); // Changed String to User
    TaskDTO getTaskById(Long id, User currentUser); // Added User
    List<TaskDTO> getAllTasks(User currentUser); // Added User, for admin overview
    List<TaskDTO> getTasksByStudentId(Long studentId, User currentUser); // Added User
    List<TaskDTO> getTasksByTeacherId(Long teacherId, User currentUser); // Added User
    List<TaskDTO> getTasksByClassId(Long classId, User currentUser); // Added User
    TaskDTO updateTask(Long taskId, UpdateTaskRequestDTO requestDTO, User currentUser); // Changed String to User
    void deleteTask(Long id, User currentUser); // Changed String to User
}
