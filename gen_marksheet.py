from reportlab.pdfgen import canvas

def create_marksheet():
    c = canvas.Canvas("C:\\Academic-Passport\\test_marksheet.pdf")
    c.drawString(100, 800, "GLOBAL UNIVERSITY - OFFICIAL MARKSHEET")
    c.drawString(100, 780, "========================================")
    c.drawString(100, 750, "Student Name: John Doe")
    c.drawString(100, 730, "Register Number: GLB-184f4af7")
    c.drawString(100, 710, "Semester: 1")
    c.drawString(100, 690, "Institution: Global Institute of Technology")
    
    c.drawString(100, 650, "Subjects & Marks:")
    c.drawString(100, 630, "Code    | Subject Name             | Marks | Total | Grade")
    c.drawString(100, 610, "--------------------------------------------------------")
    c.drawString(100, 590, "CS101   | Intro to Programming     | 85    | 100   | A")
    c.drawString(100, 570, "MA101   | Engineering Math I       | 92    | 100   | S")
    c.drawString(100, 550, "PH101   | Engineering Physics      | 78    | 100   | B")
    
    c.drawString(100, 500, "Result: PASS")
    c.drawString(100, 480, "Date: 2026-07-19")
    
    c.save()

if __name__ == "__main__":
    create_marksheet()
