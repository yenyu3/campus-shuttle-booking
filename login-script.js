document.addEventListener('DOMContentLoaded', function() {
    const cardWrapper = document.getElementById('cardWrapper');
    const showRegisterBtn = document.getElementById('showRegister');
    const showLoginBtn = document.getElementById('showLogin');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    // 切換到註冊頁面
    showRegisterBtn.addEventListener('click', function(e) {
        e.preventDefault();
        cardWrapper.classList.add('switching');
        setTimeout(() => {
            cardWrapper.classList.remove('switching');
        }, 500);
    });

    // 切換到登入頁面
    showLoginBtn.addEventListener('click', function(e) {
        e.preventDefault();
        cardWrapper.classList.add('switching');
        setTimeout(() => {
            cardWrapper.classList.remove('switching');
        }, 500);
    });

    // 登入表單提交
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const formData = new FormData(loginForm);
        const loginData = {
            studentId: formData.get('studentId'),
            password: formData.get('password')
        };

        // 發送登入請求
        fetch('/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(loginData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 登入成功，跳轉到主頁面
                localStorage.setItem('studentId', loginData.studentId);
                window.location.href = 'index.html';
            } else {
                alert('登入失敗：' + (data.message || '學號或密碼錯誤'));
            }
        })
        .catch(error => {
            console.error('登入錯誤:', error);
            alert('登入失敗，請稍後再試');
        });
    });

    // 註冊表單提交
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const formData = new FormData(registerForm);
        const password = formData.get('password');
        const confirmPassword = formData.get('confirmPassword');

        // 驗證密碼確認
        if (password !== confirmPassword) {
            alert('密碼與確認密碼不符');
            return;
        }

        const registerData = {
            studentId: formData.get('studentId'),
            name: formData.get('name'),
            email: formData.get('email'),
            password: password
        };

        // 發送註冊請求
        fetch('/api/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(registerData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('註冊成功！請使用您的學號和密碼登入');
                // 切換到登入頁面
                showLoginBtn.click();
                // 清空註冊表單
                registerForm.reset();
            } else {
                alert('註冊失敗：' + (data.message || '請檢查輸入資料'));
            }
        })
        .catch(error => {
            console.error('註冊錯誤:', error);
            alert('註冊失敗，請稍後再試');
        });
    });
});