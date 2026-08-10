import { useState } from "react";
import { createAdmin } from "../services/api";

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

        try {

            await createAdmin(adminData);

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

        } catch (error) 
        {
            console.log(error.response);
            if (error.response?.data?.errors) {
                alert(error.response.data.errors.join("\n"));
            } else if (error.response?.data?.message) {
                alert(error.response.data.message);
            } else {
                alert(JSON.stringify(error.response?.data));
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