// =====================
// Page Switching Logic
// =====================
function showPage(pageId) {
  document.querySelectorAll(".page").forEach(p => {
    p.classList.remove("active");
  });
  document.getElementById(pageId).classList.add("active");
}

// =====================
// Logout
// =====================
function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  showPage("loginPage");
}

// =====================
// Handle Login
// =====================
document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("loginUsername").value.trim();
  const password = document.getElementById("loginPassword").value.trim();

  try {
    const res = await fetch("http://localhost:8080/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) throw new Error("❌ Login failed. Check your credentials!");

    const data = await res.json();

    // Save token + user
    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify(data));

    // Update Dashboard
    document.getElementById("userName").innerText = data.username || username;
    document.getElementById("userEmail").innerText = data.email || "—";

    showPage("dashboardPage");
  } catch (err) {
    alert(err.message);
  }
});

// =====================
// Handle Register
// =====================
document.getElementById("registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("registerUsername").value.trim();
  const email = document.getElementById("registerEmail").value.trim();
  const password = document.getElementById("registerPassword").value.trim();

  try {
    const res = await fetch("http://localhost:8080/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, email, password })
    });

    if (!res.ok) throw new Error("❌ Registration failed. Try again!");

    alert("✅ Registration successful! Please login.");
    showPage("loginPage");
  } catch (err) {
    alert(err.message);
  }
});

// =====================
// Auto Login if Token exists
// =====================
window.addEventListener("DOMContentLoaded", () => {
  const token = localStorage.getItem("token");
  const user = localStorage.getItem("user");

  if (token && user) {
    const userData = JSON.parse(user);
    document.getElementById("userName").innerText = userData.username;
    document.getElementById("userEmail").innerText = userData.email || "—";
    showPage("dashboardPage");
  } else {
    showPage("loginPage");
  }
});
