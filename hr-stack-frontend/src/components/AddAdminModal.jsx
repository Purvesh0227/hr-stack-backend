import { useState } from "react";
import API from "../services/api";

import {
    isValidEmail,
    isValidPhone,
    getPasswordChecks
} from "../utils/validators";

function AddAdminModal({ isOpen, onClose, refreshAdmins }) {

    const [adminData, setAdminData] = useState({
        firstName: "",
        lastName: "",
        email: "",
        mobile: "",
        password: ""
    });

    const handleChange = (e) => {
        setAdminData({
            ...adminData,
            [e.target.name]: e.target.value
        });
    };

    // Validation - runs while typing
    const firstNameValid =
        adminData.firstName !== "" &&
        /^\S+$/.test(adminData.firstName);

    const lastNameValid =
        adminData.lastName !== "" &&
        /^\S+$/.test(adminData.lastName);

    const emailValid =
        isValidEmail(adminData.email);

    const phoneValid =
        isValidPhone(adminData.mobile);

    const {
        hasMinLength,
        hasUpperCase,
        hasLowerCase,
        hasNumber,
        hasSpecial
    } = getPasswordChecks(adminData.password);

    const passwordValid =
        hasMinLength &&
        hasUpperCase &&
        hasLowerCase &&
        hasNumber &&
        hasSpecial;

    const formValid =
        firstNameValid &&
        lastNameValid &&
        emailValid &&
        phoneValid &&
        passwordValid;

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formValid) {
            return;
        }

        try {

            await API.post("/createAdmin", adminData);

            alert("Admin created successfully");

            refreshAdmins();

            setAdminData({
                firstName: "",
                lastName: "",
                email: "",
                mobile: "",
                password: ""
            });

            onClose();

        } catch (error) {

            console.log(error.response);

            if (error.response?.data?.errors) {
                alert(error.response.data.errors.join("\n"));
            }
            else if (error.response?.data?.message) {
                alert(error.response.data.message);
            }
            else {
                alert("Failed to create admin");
            }
        }
    };

    if (!isOpen) {
        return null;
    }

    return (
        <div className="modal-overlay">

            <div className="modal">

                <h2>Add Admin</h2>

                <form onSubmit={handleSubmit}>

                    {/* First Name */}
                    <input
                        type="text"
                        name="firstName"
                        placeholder="First Name"
                        value={adminData.firstName}
                        onChange={handleChange}
                        required
                    />

                    {adminData.firstName !== "" && (
                        <small
                            className={
                                firstNameValid
                                    ? "valid"
                                    : "invalid"
                            }
                        >
                            {firstNameValid
                                ? "✓ Valid First Name"
                                : "✗ First name must not contain spaces"}
                        </small>
                    )}


                    {/* Last Name */}
                    <input
                        type="text"
                        name="lastName"
                        placeholder="Last Name"
                        value={adminData.lastName}
                        onChange={handleChange}
                        required
                    />

                    {adminData.lastName !== "" && (
                        <small
                            className={
                                lastNameValid
                                    ? "valid"
                                    : "invalid"
                            }
                        >
                            {lastNameValid
                                ? "✓ Valid Last Name"
                                : "✗ Last name must not contain spaces"}
                        </small>
                    )}


                    {/* Email */}
                    <input
                        type="email"
                        name="email"
                        placeholder="Email"
                        value={adminData.email}
                        onChange={handleChange}
                        required
                    />

                    {adminData.email !== "" && (
                        <small
                            className={
                                emailValid
                                    ? "valid"
                                    : "invalid"
                            }
                        >
                            {emailValid
                                ? "✓ Valid Email"
                                : "✗ Enter a valid email address"}
                        </small>
                    )}


                    {/* Mobile */}
                    <input
                        type="text"
                        name="mobile"
                        placeholder="Mobile Number"
                        value={adminData.mobile}
                        onChange={handleChange}
                        maxLength="10"
                        required
                    />

                    {adminData.mobile !== "" && (
                        <small
                            className={
                                phoneValid
                                    ? "valid"
                                    : "invalid"
                            }
                        >
                            {phoneValid
                                ? "✓ Valid Mobile Number"
                                : "✗ Mobile number must contain exactly 10 digits"}
                        </small>
                    )}


                    {/* Password */}
                    <input
                        type="password"
                        name="password"
                        placeholder="Password"
                        value={adminData.password}
                        onChange={handleChange}
                        required
                    />

                    {adminData.password !== "" && (
                        <div className="password-rules">

                            <p className={
                                hasMinLength
                                    ? "valid"
                                    : "invalid"
                            }>
                                {hasMinLength ? "✓" : "✗"}
                                {" "}Minimum 8 characters
                            </p>

                            <p className={
                                hasUpperCase
                                    ? "valid"
                                    : "invalid"
                            }>
                                {hasUpperCase ? "✓" : "✗"}
                                {" "}One uppercase letter
                            </p>

                            <p className={
                                hasLowerCase
                                    ? "valid"
                                    : "invalid"
                            }>
                                {hasLowerCase ? "✓" : "✗"}
                                {" "}One lowercase letter
                            </p>

                            <p className={
                                hasNumber
                                    ? "valid"
                                    : "invalid"
                            }>
                                {hasNumber ? "✓" : "✗"}
                                {" "}One number
                            </p>

                            <p className={
                                hasSpecial
                                    ? "valid"
                                    : "invalid"
                            }>
                                {hasSpecial ? "✓" : "✗"}
                                {" "}One special character
                            </p>

                        </div>
                    )}


                    {/* Buttons */}
                    <div className="modal-buttons">

                        <button
                            type="button"
                            className="cancel-btn"
                            onClick={onClose}
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            className="save-btn"
                            disabled={!formValid}
                        >
                            Save
                        </button>

                    </div>

                </form>

            </div>

        </div>
    );
}

export default AddAdminModal;