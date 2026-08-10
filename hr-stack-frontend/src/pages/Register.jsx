import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerEmployee } from "../services/api";
import { isValidEmail, isValidPhone, getPasswordChecks, doPasswordsMatch } from "../utils/validators";


function Register() {

    const navigate = useNavigate();

    const [employee, setEmployee] = useState({
        firstName: "",
        lastName: "",
        email: "",
        mobile: "",
        password: "",
        confirmPassword: ""
    });

    const handleChange = (e) => {

        setEmployee({
            ...employee,
            [e.target.name]: e.target.value
        });

    };

    // For Validation
    const emailValid = isValidEmail(employee.email);
    const phoneValid = isValidPhone(employee.mobile);
    const { hasMinLength, hasUpperCase, hasLowerCase, hasNumber, hasSpecial } = getPasswordChecks(employee.password);
    const passwordsMatch = doPasswordsMatch(employee.password, employee.confirmPassword);


    const handleRegister = async (e) => {
        e.preventDefault();

        if(!emailValid) {
            alert("Please enter a valid email.");
            return;
        }
        if(!phoneValid) {
            alert("Mobile number must contain exactly 10 digits.");
            return;
        }

        if (!hasMinLength ||!hasUpperCase ||!hasLowerCase ||!hasNumber ||!hasSpecial) {
            alert("Password does not satisfy all requirements.");
            return;
        }

        if (!passwordsMatch) {
            alert("Passwords do not match");
            return;
        }

        try {
            await registerEmployee(employee);
            alert("Employee Registered Successfully");
            navigate("/login");
        } catch (error) 
        {

            if (error.response) {
                if (error.response.data.error) {
                    alert(error.response.data.error);
                } 
                else 
                {
                const errors = error.response.data;
                let message = "";

                for (let key in errors) {
                    message += errors[key] + "\n";
                }
    
                alert(message);
                }

            } 
            else {  
                alert("Registration Failed");
            }
        }
    };
    return (
        <div className="register-container">
            <form className="register-card" onSubmit={handleRegister}>
                <h2>Employee Registration</h2>
                <input
                    type="text"
                    name="firstName"
                    placeholder="First Name"
                    value={employee.firstName}
                    onChange={handleChange}
                    required
                />

                <input
                    type="text"
                    name="lastName"
                    placeholder="Last Name"
                    value={employee.lastName}
                    onChange={handleChange}
                    required
                />

                <input
                    type="email"
                    name="email"
                    placeholder="Email"
                    value={employee.email}
                    onChange={handleChange}
                    required
                />
                {employee.email !== "" && (
                    <small className={emailValid ? "valid" : "invalid"}>{emailValid ? "✓ Valid Email": "✗ Enter a valid email address"}  </small>
                )}

                <input
                    type="text"
                    name="mobile"
                    placeholder="Mobile Number"
                    value={employee.mobile}
                    onChange={handleChange}
                    maxLength="10"
                    required
                />

                {employee.mobile !== "" && ( 
                    <small className={phoneValid ? "valid" : "invalid"}> {phoneValid ? "✓ Valid Mobile Number" : "✗ Mobile number must contain exactly 10 digits"}
                </small>
                )}

                <input
                    type="password"
                    name="password"
                    placeholder="Password"
                    value={employee.password}
                    onChange={handleChange}
                    required
                />
                <div className="password-rules">

                <p className={hasMinLength ? "valid" : "invalid"}> {hasMinLength ? "✓":"✗"} Minimum 8 characters
                </p>

                <p className={hasUpperCase ? "valid" : "invalid"}>
                {hasUpperCase ? "✓":"✗"} One uppercase letter
                </p>

                <p className={hasLowerCase ? "valid" : "invalid"}>
                {hasLowerCase ? "✓":"✗"} One lowercase letter
                </p>

                <p className={hasNumber ? "valid" : "invalid"}>
                {hasNumber ? "✓" : "✗"} One number
                </p>

                <p className={hasSpecial ? "valid" : "invalid"}>
                {hasSpecial ? "✓" : "✗"} One special character
                </p>

                </div>
                <input
                    type="password"
                    name="confirmPassword"
                    placeholder="Confirm Password"
                    value = {employee.confirmPassword}
                    onChange={handleChange}
                    required
                />

                {employee.confirmPassword !== "" && (
                    <p className={passwordsMatch ? "valid" : "invalid"}> {passwordsMatch ? "✓ Passwords match" : "✗ Passwords do not match"}
                </p>    
                )}


                 <button type="submit" disabled={!passwordsMatch || !emailValid ||!phoneValid ||!hasMinLength ||!hasUpperCase ||!hasLowerCase ||!hasNumber ||!hasSpecial}>
                    Register
                </button>

                <p>Already have an account? <Link to="/login">Login</Link>
                </p>
            </form>
        </div>
    );
}

export default Register;