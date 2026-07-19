package com.academicpassport.marksheet;

import com.academicpassport.auth.User;
import com.academicpassport.auth.UserRepository;
import com.academicpassport.auth.UserRole;
import com.academicpassport.auth.UserPrincipal;
import com.academicpassport.config.AbstractIntegrationTest;
import com.academicpassport.college.College;
import com.academicpassport.college.CollegeRepository;
import com.academicpassport.college.Department;
import com.academicpassport.college.DepartmentRepository;
import com.academicpassport.college.CollegeInvitationRepository;
import com.academicpassport.college.Semester;
import com.academicpassport.college.SemesterRepository;
import com.academicpassport.student.Student;
import com.academicpassport.student.StudentRepository;
import com.academicpassport.storage.FileStorageService;
import com.academicpassport.marksheet.service.ClamAVService;
import com.academicpassport.marksheet.service.OcrProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;

public class MarksheetUploadIntegrationTest extends AbstractIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MarksheetRepository marksheetRepository;

    @Autowired
    private CollegeInvitationRepository collegeInvitationRepository;

    @Autowired
    private OcrResultRepository ocrResultRepository;

    @MockitoBean
    private ClamAVService clamAVService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private OcrProcessingService ocrProcessingService;

    private Student studentA;
    private User userA;
    private Student studentB;
    private User userB;
    private Semester semester1;
    private Semester semester2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        marksheetRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        collegeInvitationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        semesterRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();
        collegeRepository.deleteAllInBatch();

        College collegeA = new College();
        collegeA.setName("College A");
        collegeA.setCollegeCode("CA-100");
        collegeA.setAddressLine("123 Main St");
        collegeA = collegeRepository.save(collegeA);

        Department deptA = new Department();
        deptA.setCollege(collegeA);
        deptA.setName("Computer Science");
        deptA.setCode("CS");
        deptA = departmentRepository.save(deptA);

        semester1 = new Semester();
        semester1.setCollege(collegeA);
        semester1.setDepartment(deptA);
        semester1.setSemesterNumber(1);
        semester1 = semesterRepository.save(semester1);

        semester2 = new Semester();
        semester2.setCollege(collegeA);
        semester2.setDepartment(deptA);
        semester2.setSemesterNumber(2);
        semester2 = semesterRepository.save(semester2);

        userA = new User();
        userA.setCollege(collegeA);
        userA.setEmail("alice@test.com");
        userA.setPasswordHash("hash");
        userA.setRole(UserRole.STUDENT);
        userA = userRepository.save(userA);

        studentA = new Student();
        studentA.setUser(userA);
        studentA.setCollege(collegeA);
        studentA.setDepartment(deptA);
        studentA.setFullName("Alice Smith");
        studentA.setRollNumber("101");
        studentA.setUniversityRegisterNo("REG-001");
        studentA.setDob(LocalDate.of(2000, 1, 1));
        studentA.setBatchYear(2023);
        studentA.setCurrentSemester(4);
        studentA = studentRepository.save(studentA);

        College collegeB = new College();
        collegeB.setName("College B");
        collegeB.setCollegeCode("CB-200");
        collegeB.setAddressLine("456 Elm St");
        collegeB = collegeRepository.save(collegeB);

        Department deptB = new Department();
        deptB.setCollege(collegeB);
        deptB.setName("Mechanical");
        deptB.setCode("ME");
        deptB = departmentRepository.save(deptB);

        userB = new User();
        userB.setCollege(collegeB);
        userB.setEmail("bob@test.com");
        userB.setPasswordHash("hash");
        userB.setRole(UserRole.STUDENT);
        userB = userRepository.save(userB);

        studentB = new Student();
        studentB.setUser(userB);
        studentB.setCollege(collegeB);
        studentB.setDepartment(deptB);
        studentB.setFullName("Bob Jones");
        studentB.setRollNumber("102");
        studentB.setUniversityRegisterNo("REG-002");
        studentB.setDob(LocalDate.of(2000, 1, 1));
        studentB.setBatchYear(2023);
        studentB.setCurrentSemester(2);
        studentB = studentRepository.save(studentB);

        when(fileStorageService.storeFile(any(), any(), any(), any())).thenReturn("mocked/path/file.pdf");
        doNothing().when(clamAVService).scan(any());
    }

    private byte[] generateValidPdfBytes() {
        return "%PDF-1.4\n%EOF".getBytes();
    }
    
    private byte[] generateInvalidBytes() {
        return "This is just a text file".getBytes();
    }

    @Test
    void testValidUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", generateValidPdfBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/student/marksheets")
                .file(file)
                .param("semesterId", semester1.getId().toString())
                .with(user(new UserPrincipal(userA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semesterId").value(semester1.getId()))
                .andExpect(jsonPath("$.status").value("Uploaded — awaiting processing"));

        Optional<Marksheet> marksheet = marksheetRepository.findByStudentIdAndSemesterId(studentA.getId(), semester1.getId());
        assertThat(marksheet).isPresent();
        assertThat(marksheet.get().getVirusScanStatus()).isEqualTo(ScanStatus.CLEAN);

        Optional<OcrResult> ocr = ocrResultRepository.findByMarksheetId(marksheet.get().getId());
        assertThat(ocr).isPresent();
        assertThat(ocr.get().getStatus()).isEqualTo(OcrStatus.PENDING);
        
        verify(clamAVService, times(1)).scan(any());
    }

    @Test
    void testDuplicateUploadFailsWith409() throws Exception {
        testValidUpload(); 

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", generateValidPdfBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/student/marksheets")
                .file(file)
                .param("semesterId", semester1.getId().toString())
                .with(user(new UserPrincipal(userA))))
                .andExpect(status().isConflict());
    }

    @Test
    void testSpoofedExtensionRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", generateInvalidBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/student/marksheets")
                .file(file)
                .param("semesterId", semester1.getId().toString())
                .with(user(new UserPrincipal(userA))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testClamAVRejection() throws Exception {
        doThrow(new SecurityException("Malware detected")).when(clamAVService).scan(any());
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", generateValidPdfBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/student/marksheets")
                .file(file)
                .param("semesterId", semester1.getId().toString())
                .with(user(new UserPrincipal(userA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReplacementFlow() throws Exception {
        testValidUpload();
        Marksheet oldMarksheet = marksheetRepository.findByStudentIdAndSemesterId(studentA.getId(), semester1.getId()).get();
        
        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", generateValidPdfBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/v1/student/marksheets/" + oldMarksheet.getId() + "/file")
                .file(file)
                .with(user(new UserPrincipal(userA))))
                .andExpect(status().isOk());
        
        verify(fileStorageService, times(1)).deleteFile("mocked/path/file.pdf");
    }
}
