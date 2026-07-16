package com.academicpassport.auth;

import com.academicpassport.college.College;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationScopeServiceTest {

    private AuthorizationScopeService service;
    private User mockUser;

    @BeforeEach
    void setUp() {
        service = new AuthorizationScopeService();
        mockUser = mock(User.class);
    }

    @Test
    void testVerifySelfOwnership_SameUser_Success() {
        when(mockUser.getId()).thenReturn(10L);
        when(mockUser.getRole()).thenReturn(UserRole.STUDENT);
        UserPrincipal principal = new UserPrincipal(mockUser);

        assertDoesNotThrow(() -> service.verifySelfOwnership(10L, principal));
    }

    @Test
    void testVerifySelfOwnership_DifferentUser_Denied() {
        when(mockUser.getId()).thenReturn(10L);
        when(mockUser.getRole()).thenReturn(UserRole.STUDENT);
        UserPrincipal principal = new UserPrincipal(mockUser);

        assertThrows(AccessDeniedException.class, () -> service.verifySelfOwnership(11L, principal));
    }

    @Test
    void testVerifySelfOwnership_SuperAdmin_Success() {
        when(mockUser.getId()).thenReturn(10L);
        when(mockUser.getRole()).thenReturn(UserRole.SUPER_ADMIN);
        UserPrincipal principal = new UserPrincipal(mockUser);

        assertDoesNotThrow(() -> service.verifySelfOwnership(99L, principal));
    }

    @Test
    void testVerifySameCollege_SameCollege_Success() {
        College college = new College();
        college.setId(5L);
        when(mockUser.getRole()).thenReturn(UserRole.STAFF);
        when(mockUser.getCollege()).thenReturn(college);
        UserPrincipal principal = new UserPrincipal(mockUser);

        assertDoesNotThrow(() -> service.verifySameCollege(5L, principal));
    }

    @Test
    void testVerifySameCollege_DifferentCollege_Denied() {
        College college = new College();
        college.setId(5L);
        when(mockUser.getRole()).thenReturn(UserRole.STAFF);
        when(mockUser.getCollege()).thenReturn(college);
        UserPrincipal principal = new UserPrincipal(mockUser);

        assertThrows(AccessDeniedException.class, () -> service.verifySameCollege(6L, principal));
    }
}
