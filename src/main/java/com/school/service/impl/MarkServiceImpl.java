package com.school.service.impl;

import com.school.dto.*;
import com.school.entity.*;
import com.school.exception.ResourceNotFoundException;
import com.school.exception.UnauthorizedActionException;
import com.school.repository.*;
import com.school.service.MarkService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final SubjectAssignmentRepository subjectAssignmentRepository;
    private final TeacherRepository teacherRepository; // For Teacher Profile
    private final UserRepository userRepository; // Added for getMarksByStudentUserId
    private final ModelMapper modelMapper;
    private final UserServiceImpl userServiceImpl; // For UserDTO in nested DTOs
    private final SubjectAssignmentServiceImpl subjectAssignmentServiceImpl; // For SubjectAssignmentResponseDTO

    @Override
    @Transactional
    public MarkResponseDTO enterMarks(EnterMarkRequestDTO requestDTO, User currentUser) {
        Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("User does not have a teacher profile."));

        Student student = studentRepository.findById(requestDTO.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + requestDTO.getStudentId()));

        SubjectAssignment subjectAssignment = subjectAssignmentRepository.findById(requestDTO.getSubjectAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject Assignment not found: " + requestDTO.getSubjectAssignmentId()));

        // Authorization:
        // 1. Class teacher of the student's current class can enter marks for any subject assignment of that class.
        // 2. The specific subject teacher for that subject_assignment can enter marks.
        Classes studentClass = student.getClasses();
        boolean isClassTeacher = studentClass.getClassTeacher() != null && studentClass.getClassTeacher().getId().equals(currentUser.getId());
        boolean isSubjectTeacherForAssignment = subjectAssignment.getTeacher().getId().equals(currentTeacherProfile.getId());

        if (!isClassTeacher && !isSubjectTeacherForAssignment) {
            throw new UnauthorizedActionException("Teacher is not authorized to enter marks for this student/subject assignment.");
        }
        // Ensure student belongs to the class of the subject assignment
        if (!student.getClasses().getId().equals(subjectAssignment.getClasses().getId())) {
             throw new IllegalArgumentException("Student does not belong to the class of this subject assignment.");
        }
         // Ensure subject assignment is for the student's class
        if (!subjectAssignment.getClasses().getId().equals(studentClass.getId())) {
            throw new IllegalArgumentException("Subject assignment does not match the student's class.");
        }


        Mark mark = Mark.builder()
                .student(student)
                .subjectAssignment(subjectAssignment)
                .assessmentName(requestDTO.getAssessmentName())
                .marksObtained(requestDTO.getMarksObtained())
                .totalMarks(requestDTO.getTotalMarks())
                .grade(requestDTO.getGrade())
                .examDate(requestDTO.getExamDate())
                .comments(requestDTO.getComments())
                .recordedByTeacher(currentTeacherProfile)
                .build();

        Mark savedMark = markRepository.save(mark);
        return convertToDTO(savedMark);
    }

    @Override
    @Transactional
    public MarkResponseDTO updateMarks(Long markId, EnterMarkRequestDTO requestDTO, User currentUser) {
        Mark mark = markRepository.findById(markId)
                .orElseThrow(() -> new ResourceNotFoundException("Mark record not found: " + markId));

        Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("User does not have a teacher profile."));

        // Authorization: Only the teacher who originally recorded it, or the class teacher, or Admin/SuperAdmin.
        boolean isOriginalRecorder = mark.getRecordedByTeacher() != null && mark.getRecordedByTeacher().getId().equals(currentTeacherProfile.getId());
        boolean isClassTeacher = mark.getStudent().getClasses().getClassTeacher() != null &&
                                 mark.getStudent().getClasses().getClassTeacher().getId().equals(currentUser.getId());

        if (!isOriginalRecorder && !isClassTeacher && !(currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN)) {
             throw new UnauthorizedActionException("User not authorized to update this mark record.");
        }

        // Ensure student and subject assignment are not being changed via this update method for simplicity.
        // If they need to be changed, it might imply deleting and re-creating the mark.
        if (!mark.getStudent().getId().equals(requestDTO.getStudentId()) ||
            !mark.getSubjectAssignment().getId().equals(requestDTO.getSubjectAssignmentId())) {
            throw new IllegalArgumentException("Student or Subject Assignment cannot be changed during mark update. Delete and re-create if necessary.");
        }

        mark.setAssessmentName(requestDTO.getAssessmentName());
        mark.setMarksObtained(requestDTO.getMarksObtained());
        mark.setTotalMarks(requestDTO.getTotalMarks());
        mark.setGrade(requestDTO.getGrade());
        mark.setExamDate(requestDTO.getExamDate());
        mark.setComments(requestDTO.getComments());
        mark.setRecordedByTeacher(currentTeacherProfile); // Update who last modified/recorded

        Mark updatedMark = markRepository.save(mark);
        return convertToDTO(updatedMark);
    }

    @Override
    public MarkResponseDTO getMarkById(Long markId, User currentUser) {
        Mark mark = markRepository.findById(markId)
                .orElseThrow(() -> new ResourceNotFoundException("Mark record not found: " + markId));
        authorizeViewMark(mark, currentUser, null);
        return convertToDTO(mark);
    }

    @Override
    public List<MarkResponseDTO> getMarksByStudentId(Long studentId, User currentUser) { // Student Profile ID
        Student student = studentRepository.findById(studentId)
             .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + studentId));
        authorizeViewMark(null, currentUser, student); // General check for student data access
        List<Mark> marks = markRepository.findByStudentId(studentId);
        return marks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<MarkResponseDTO> getMarksByStudentUserId(Long studentUserId, User currentUser) { // Main User ID
        User studentUserAccount = userRepository.findById(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student user account not found: " + studentUserId));
        Student student = studentRepository.findByUserId(studentUserAccount.getId())
             .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + studentUserId));
        authorizeViewMark(null, currentUser, student);
        List<Mark> marks = markRepository.findByStudent_User_Id(studentUserId);
        return marks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }


    @Override
    public List<MarkResponseDTO> getMarksBySubjectAssignmentId(Long subjectAssignmentId, User currentUser) {
         SubjectAssignment sa = subjectAssignmentRepository.findById(subjectAssignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject Assignment not found: " + subjectAssignmentId));
        // Authorization: Teacher of SA, Class Teacher of SA's class, Admin, SuperAdmin
        authorizeViewMarkForSA(sa, currentUser);
        List<Mark> marks = markRepository.findBySubjectAssignmentId(subjectAssignmentId);
        return marks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMark(Long markId, User currentUser){
        Mark mark = markRepository.findById(markId)
                .orElseThrow(() -> new ResourceNotFoundException("Mark record not found: " + markId));
        // Authorization: Similar to update: original recorder, class teacher, Admin, SuperAdmin
        Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId()).orElse(null);
        boolean isOriginalRecorder = currentTeacherProfile != null && mark.getRecordedByTeacher() != null && mark.getRecordedByTeacher().getId().equals(currentTeacherProfile.getId());
        boolean isClassTeacher = mark.getStudent().getClasses().getClassTeacher() != null &&
                                 mark.getStudent().getClasses().getClassTeacher().getId().equals(currentUser.getId());

        if (!isOriginalRecorder && !isClassTeacher && !(currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN)) {
             throw new UnauthorizedActionException("User not authorized to delete this mark record.");
        }
        markRepository.delete(mark);
    }

    private void authorizeViewMark(Mark mark, User currentUser, Student targetStudentIfMarkIsNull) {
        Student studentToAuthorize = (mark != null) ? mark.getStudent() : targetStudentIfMarkIsNull;
        if (studentToAuthorize == null) throw new IllegalArgumentException("Target student for authorization cannot be null.");

        if (currentUser.getRole() == UserRole.SUPER_ADMIN) return;
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() != null && studentToAuthorize.getClasses().getSchool().getId().equals(currentUser.getSchoolId())) return;
        }
        if (currentUser.getRole() == UserRole.STUDENT && studentToAuthorize.getUser().getId().equals(currentUser.getId())) return;

        if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("Teacher profile not found for current user."));

            if (studentToAuthorize.getClasses().getClassTeacher() != null && studentToAuthorize.getClasses().getClassTeacher().getId().equals(currentUser.getId())) return;

            if (mark != null && mark.getSubjectAssignment().getTeacher().getId().equals(currentTeacherProfile.getId())) return; // Teacher of specific subject assignment

            // Check if teacher is actively teaching the student's class (general subject teacher)
             if (subjectAssignmentRepository.existsByClassesIdAndTeacherIdAndStatus(studentToAuthorize.getClasses().getId(), currentTeacherProfile.getId(), "ACTIVE")) return;
        }
        throw new UnauthorizedActionException("User is not authorized to view this mark information.");
    }

    private void authorizeViewMarkForSA(SubjectAssignment sa, User currentUser) {
         if (currentUser.getRole() == UserRole.SUPER_ADMIN) return;
         if (currentUser.getRole() == UserRole.ADMIN) {
            if (currentUser.getSchoolId() != null && sa.getClasses().getSchool().getId().equals(currentUser.getSchoolId())) return;
         }
         if (currentUser.getRole() == UserRole.TEACHER) {
            Teacher currentTeacherProfile = teacherRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("Teacher profile not found for current user."));
            if (sa.getTeacher().getId().equals(currentTeacherProfile.getId())) return; // Teacher of this SA
            if (sa.getClasses().getClassTeacher() != null && sa.getClasses().getClassTeacher().getId().equals(currentUser.getId())) return; // Class teacher of SA's class
         }
         // Students view their marks through student-specific endpoints.
         throw new UnauthorizedActionException("User is not authorized to view marks for this subject assignment directly.");
    }

    private MarkResponseDTO convertToDTO(Mark mark) {
        MarkResponseDTO dto = modelMapper.map(mark, MarkResponseDTO.class);
        if (mark.getStudent() != null) {
            StudentDTO studentDTO = modelMapper.map(mark.getStudent(), StudentDTO.class);
            if(mark.getStudent().getUser() != null)
                studentDTO.setUser(userServiceImpl.convertToDTO(mark.getStudent().getUser()));
            if(mark.getStudent().getClasses() != null){
                studentDTO.setClassName(mark.getStudent().getClasses().getName());
                studentDTO.setClassId(mark.getStudent().getClasses().getId());
            }
            dto.setStudent(studentDTO);
        }
        if (mark.getSubjectAssignment() != null) {
            dto.setSubjectAssignment(subjectAssignmentServiceImpl.convertToResponseDTO(mark.getSubjectAssignment()));
        }
        if (mark.getRecordedByTeacher() != null) {
             TeacherDTO teacherDTO = modelMapper.map(mark.getRecordedByTeacher(), TeacherDTO.class);
             if(mark.getRecordedByTeacher().getUser() != null)
                teacherDTO.setUser(userServiceImpl.convertToDTO(mark.getRecordedByTeacher().getUser()));
            dto.setRecordedByTeacher(teacherDTO);
        }
        return dto;
    }
}
