import axios from 'axios';
import {jwtDecode} from 'jwt-decode';

const API_URL = "http://localhost:8080/api/v1/auth/";


// Register user
const register = async (userData) => {
  const response = await axios.post(`${API_URL}register`, userData,{
    headers: {
      'Content-Type': 'mutipart/form-data'
    }
  })
  return response.data;
};

// Login user
const login = async (userData) => {
  const response = await axios.post(`${API_URL}login`, userData);
  if (response.data && response.data.accessToken) {
    const decodedToken = jwtDecode(response.data.accessToken);
    const user ={
      ...response.data,
      role: decodedToken.role
    };
    localStorage.setItem('user', JSON.stringify(user));
    return user;
  }
}


// Logout user
const logout = () => {
  localStorage.removeItem('user');
}


const authService = {
  register,
  logout,
  login,
};

export default authService;