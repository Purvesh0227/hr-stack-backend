const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_REGEX = /^[0-9]{10}$/;

export const isValidEmail = (email) => EMAIL_REGEX.test(email);
export const isValidPhone = (phone) => PHONE_REGEX.test(phone);

export const getPasswordChecks = (password) => ({

    hasMinLength: password.length >= 8,
    hasUpperCase: /[A-Z]/.test(password),
    hasLowerCase: /[a-z]/.test(password),
    hasNumber: /\d/.test(password),
    hasSpecial: /[@$!%*?&]/.test(password),
});

export const doPasswordsMatch = (password, confirmPassword) =>
    password === confirmPassword && confirmPassword!="";