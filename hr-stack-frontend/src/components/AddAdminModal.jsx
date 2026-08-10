import { useState } from "react";
import API from "../services/api";
import {isValidEmail,isValidPhone,getPasswordChecks} from "../utils/validators";

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

    const handleSubmit = async (e) => {
    e.preventDefault();

    if (!/^\S+$/.test(adminData.firstName)) {
        alert("First name must not contain spaces");
        return;
    }

    if (!/^\S+$/.test(adminData.lastName)) {
        alert("Last name must not contain spaces");
        return;
    }

    if (!isValidEmail(adminData.email)) {
        alert("Please enter a valid email");
        return;
    }

    if (!isValidPhone(adminData.mobile)) {
        alert("Mobile number must contain exactly 10 digits");
        return;
    }

    const passwordChecks = getPasswordChecks(adminData.password);

    if (
        !passwordChecks.hasMinLength ||
        !passwordChecks.hasUpperCase ||
        !passwordChecks.hasLowerCase ||
        !passwordChecks.hasNumber ||
        !passwordChecks.hasSpecial
    ) {
        alert(
            "Password must be at least 8 characters and include uppercase, lowercase, number and special character"
        );
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
    console.error("Create Admin Error:", error);

    if (error.response) {
        console.error("Status:", error.response.status);
        console.error("Response:", error.response.data);

        if (error.response.data?.errors) {
            alert(error.response.data.errors.join("\n"));
        } else if (error.response.data?.error) {
            alert(error.response.data.error);
        } else if (error.response.data?.message) {
            alert(error.response.data.message);
        } else {
            alert("Failed to create admin");
        }
    } else if (error.request) {
        console.error("No response received:", error.request);
        alert("Backend server is not responding");
    } else {
        console.error("Request Error:", error.message);
        alert(error.message);
    }
}
};

    if (!isOpen) return null;

    return (
        <div className="modal-overlay">

            <div className="modal">

                <h2>Add Admin</h2>

                <form onSubmit={handleSubmit}>

                    <input
                        type="text"
                        name="firstName"
                        placeholder="First Name"
                        value={adminData.firstName}
                        onChange={handleChange}
                        required
                    />

                    <input
                        type="text"
                        name="lastName"
                        placeholder="Last Name"
                        value={adminData.lastName}
                        onChange={handleChange}
                        required
                    />

                    <input
                        type="email"
                        name="email"
                        placeholder="Email"
                        value={adminData.email}
                        onChange={handleChange}
                        required
                    />


                    <input
                    type="text"
                    name="mobile"
                    placeholder="Mobile Number"
                    value={adminData.mobile}
                    onChange={handleChange}
                    maxLength={10}
                    required
                    />

                    <input
                        type="password"
                        name="password"
                        placeholder="Password"
                        value={adminData.password}
                        onChange={handleChange}
                        required
                    />

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