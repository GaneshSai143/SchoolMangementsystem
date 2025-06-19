package com.school.service;

import com.school.dto.CreateTaskRequestDTO;
import com.school.dto.TaskDTO;
import com.school.dto.UpdateTaskRequestDTO;
import java.util.List;

public interface TaskService {
    TaskDTO createTask(CreateTaskRequestDTO requestDTO, String authenticatedUsername);
    TaskDTO getTaskById(Long id);
    List<TaskDTO> getAllTasks(); // For admin overview
    List<TaskDTO> getTasksByStudentId(Long studentId);
    List<TaskDTO> getTasksByTeacherId(Long teacherId);
    List<TaskDTO> getTasksByClassId(Long classId);
    TaskDTO updateTask(Long taskId, UpdateTaskRequestDTO requestDTO, String authenticatedUsername);
    void deleteTask(Long id, String authenticatedUsername);
}
