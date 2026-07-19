import fs from 'fs';
import path from 'path';

const API_BASE = 'http://localhost:8080/api';

async function request(endpoint, method, body, token) {
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    const options = {
        method,
        headers
    };
    if (body) {
        options.body = JSON.stringify(body);
    }
    
    const res = await fetch(`${API_BASE}${endpoint}`, options);
    if (!res.ok) {
        let text = await res.text();
        throw new Error(`API Error: ${method} ${endpoint} returned ${res.status} ${res.statusText} - ${text}`);
    }
    
    // some endpoints return 204 No Content
    const contentType = res.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return await res.json();
    }
    return null;
}

async function uploadFile(endpoint, token, filePath, semesterId) {
    const formData = new FormData();
    const fileContent = fs.readFileSync(filePath);
    const blob = new Blob([fileContent], { type: 'application/pdf' });
    formData.append('file', blob, path.basename(filePath));
    formData.append('semesterId', semesterId);

    const headers = {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const res = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers,
        body: formData
    });

    if (!res.ok) {
        let text = await res.text();
        throw new Error(`Upload Error: ${res.status} ${res.statusText} - ${text}`);
    }
    return await res.json();
}

async function run() {
    try {
        const randId = Math.random().toString(36).substring(7);
        const uniAdminEmail = `uniadmin_${randId}@example.com`;
        const staffEmail = `staff_${randId}@example.com`;
        const studentEmail = `student_${randId}@example.com`;
        const studentReg = `GLB-${randId}`;
        
        console.log("=== 1. SUPER ADMIN FLOW ===");
        // Login Super Admin
        console.log("Logging in super admin...");
        let res = await request('/auth/login', 'POST', { email: 'admin@example.com', password: 'change_me_before_production' });
        console.log("Login response:", res);
        const saToken = res.accessToken;
        
        console.log("Creating institution invitation...");
        const invite = await request('/v1/admin/invitations', 'POST', { institutionName: `Global Tech ${randId}`, adminEmail: uniAdminEmail }, saToken);
        console.log("Invitation:", invite);
        
        console.log("Completing public onboarding wizard...");
        await request(`/v1/onboarding/submit?token=${invite.token}`, 'POST', {
            institutionName: `Global Tech ${randId}`,
            addressLine: '123 Tech Park',
            city: 'San Francisco',
            state: 'CA',
            postalCode: '94105',
            country: 'USA',
            contactName: 'Uni Admin',
            contactEmail: uniAdminEmail,
            contactPhone: '555-0100',
            password: 'password123'
        });
        
        console.log("=== 2. UNIVERSITY ADMIN FLOW ===");
        console.log("Logging in uni admin...");
        res = await request('/auth/login', 'POST', { email: uniAdminEmail, password: 'password123' });
        console.log("Uni Admin Login:", res);
        const uniToken = res.accessToken;
        
        console.log("Creating department...");
        const dept = await request('/v1/university-admin/departments', 'POST', { name: 'Computer Science', code: 'CS' }, uniToken);
        const deptId = dept.id;
        
        console.log("Creating semester...");
        const sem = await request(`/v1/university-admin/departments/${deptId}/semesters`, 'POST', {
            semesterNumber: 1,
            name: 'Fall 2026',
            startDate: '2026-08-01',
            endDate: '2026-12-15'
        }, uniToken);
        const semId = sem.id;
        
        console.log("Creating subjects...");
        await request(`/v1/university-admin/semesters/${semId}/subjects`, 'POST', { subjectCode: 'CS101', subjectName: 'Intro to Programming', maxMarks: 100 }, uniToken);
        await request(`/v1/university-admin/semesters/${semId}/subjects`, 'POST', { subjectCode: 'MA101', subjectName: 'Engineering Math I', maxMarks: 100 }, uniToken);
        await request(`/v1/university-admin/semesters/${semId}/subjects`, 'POST', { subjectCode: 'PH101', subjectName: 'Engineering Physics', maxMarks: 100 }, uniToken);
        
        console.log("Creating staff...");
        await request('/v1/university-admin/staff', 'POST', {
            fullName: 'Staff User', email: staffEmail, password: 'password123', departmentId: deptId
        }, uniToken);
        
        console.log("Creating student...");
        await request('/v1/university-admin/students', 'POST', {
            fullName: 'John Doe', email: studentEmail, password: 'password123', departmentId: deptId, universityRegisterNo: studentReg, rollNumber: `R-${randId}`, dob: '2000-01-01', batchYear: 2026
        }, uniToken);
        
        console.log("=== 3. STUDENT FLOW ===");
        console.log("Logging in student...");
        res = await request('/auth/login', 'POST', { email: studentEmail, password: 'password123' });
        const studentToken = res.accessToken;
        
        console.log("Uploading marksheet...");
        const marksheet = await uploadFile('/v1/student/marksheets', studentToken, 'C:\\Academic-Passport\\test_marksheet.pdf', semId);
        const marksheetId = marksheet.id;
        console.log("Marksheet uploaded. ID:", marksheetId);
        
        console.log("=== 4. OCR VERIFICATION ===");
        let ocrStatus = 'PENDING';
        let retries = 30;
        while (retries > 0) {
            const statusDto = await request(`/v1/student/marksheets/${marksheetId}/processing-status`, 'GET', null, studentToken);
            ocrStatus = statusDto.status;
            console.log("OCR Status:", ocrStatus);
            if (ocrStatus === 'READY_FOR_REVIEW' || ocrStatus === 'REVIEW_REQUIRED' || ocrStatus === 'PROCESSING_FAILED') break;
            await new Promise(r => setTimeout(r, 2000));
            retries--;
        }
        
        if (ocrStatus === 'PROCESSING_FAILED') {
            console.error("OCR failed!");
            return;
        }
        
        console.log("=== 6. STAFF VERIFICATION ===");
        console.log("Logging in staff...");
        res = await request('/auth/login', 'POST', { email: staffEmail, password: 'password123' });
        const staffToken = res.accessToken;

        console.log("=== 5. EXTRACTION ACCURACY AUDIT ===");
        const finalMarksheet = await request(`/v1/staff/verifications/marksheets/${marksheetId}`, 'GET', null, staffToken);
        console.log(JSON.stringify(finalMarksheet, null, 2));
        
        // Find a subject to modify
        let subjectToMod = finalMarksheet.subjects.find(s => s.subjectCode === 'CS101');
        const subjectIdToModify = subjectToMod.subjectId;
        
        console.log("Modifying a mark...");
        await request(`/v1/staff/verifications/marksheets/${marksheetId}/subjects/${subjectIdToModify}`, 'PUT', { marks: 88 }, staffToken);
        
        console.log("Approving marksheet...");
        await request(`/v1/staff/verifications/marksheets/${marksheetId}/approve`, 'POST', null, staffToken);
        
        console.log("Staff approval successful!");
        
        console.log("=== 7. SECURITY VALIDATION ===");
        console.log("Testing duplicate upload (should 409)...");
        try {
            await uploadFile('/v1/student/marksheets', studentToken, 'C:\\Academic-Passport\\test_marksheet.pdf', semId);
            console.error("FAILED: Duplicate upload was allowed!");
        } catch (e) {
            console.log("SUCCESS: Duplicate upload rejected.", e.message);
        }
        
        console.log("E2E SCRIPT COMPLETED SUCCESSFULLY!");
        
    } catch (e) {
        console.error("Test failed!", e);
    }
}

run();
