package com.academicpassport.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationScopeService {

    public void verifySelfOwnership(Long requestedUserId, UserPrincipal currentUser) {
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        
        if (!requestedUserId.equals(currentUser.getId())) {
            throw new AccessDeniedException("You cannot access another user's data");
        }
    }

    public void verifySameCollege(Long requestedCollegeId, UserPrincipal currentUser) {
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        if (currentUser.getCollegeId() == null || !currentUser.getCollegeId().equals(requestedCollegeId)) {
            throw new AccessDeniedException("You do not have access to this college's resources");
        }
    }
}
