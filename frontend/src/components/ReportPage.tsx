import React, { useState, useEffect } from 'react';

const ReportPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [checkingAuth, setCheckingAuth] = useState(true);

  // Проверяем статус аутентификации при загрузке компонента
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const response = await fetch(`${process.env.REACT_APP_AUTH_API_URL}/api/auth/status`, {
        credentials: 'include' // Важно: отправляем cookies
      });
      
      if (response.ok) {
        const data = await response.json();
        setAuthenticated(data.authenticated);
      } else {
        setAuthenticated(false);
      }
    } catch (err) {
      console.error('Error checking auth status:', err);
      setAuthenticated(false);
    } finally {
      setCheckingAuth(false);
    }
  };

  const handleLogin = () => {
    // Перенаправляем на auth-api login endpoint
    window.location.href = `${process.env.REACT_APP_AUTH_API_URL}/api/auth/login`;
  };

  const handleLogout = async () => {
    try {
      await fetch(`${process.env.REACT_APP_AUTH_API_URL}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      });
      setAuthenticated(false);
      setError(null);
    } catch (err) {
      console.error('Error logging out:', err);
      setError('Logout failed');
    }
  };

  const downloadReport = async () => {
    if (!authenticated) {
      setError('Not authenticated');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Запрос к API с отправкой cookies
      const response = await fetch(`${process.env.REACT_APP_API_URL}/reports`, {
        credentials: 'include' // Автоматически отправляет session cookie
      });

      if (!response.ok) {
        if (response.status === 401) {
          // Сессия истекла или невалидна
          setAuthenticated(false);
          setError('Session expired. Please login again.');
          return;
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // Обработка успешного ответа (например, скачивание файла)
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'report.pdf';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  if (checkingAuth) {
    return <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">Loading...</div>;
  }

  if (!authenticated) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
        <div className="p-8 bg-white rounded-lg shadow-md">
          <h1 className="text-2xl font-bold mb-6">Authentication Required</h1>
          <p className="mb-6 text-gray-600">Please login to access the reports system.</p>
          <button
            onClick={handleLogin}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            Login with Keycloak
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
      <div className="p-8 bg-white rounded-lg shadow-md w-full max-w-md">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold">Usage Reports</h1>
          <button
            onClick={handleLogout}
            className="px-3 py-1 text-sm bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
          >
            Logout
          </button>
        </div>
        
        <div className="mb-6 p-4 bg-green-50 text-green-700 rounded">
          <p className="font-medium">Authenticated</p>
          <p className="text-sm">Session is active. Cookies are being used for authentication.</p>
        </div>
        
        <button
          onClick={downloadReport}
          disabled={loading}
          className={`w-full px-4 py-3 bg-blue-500 text-white rounded hover:bg-blue-600 font-medium ${
            loading ? 'opacity-50 cursor-not-allowed' : ''
          }`}
        >
          {loading ? 'Generating Report...' : 'Download Report'}
        </button>

        {error && (
          <div className="mt-4 p-4 bg-red-100 text-red-700 rounded">
            <p className="font-medium">Error</p>
            <p>{error}</p>
            {error.includes('expired') && (
              <button
                onClick={handleLogin}
                className="mt-2 px-3 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600"
              >
                Login Again
              </button>
            )}
          </div>
        )}

        <div className="mt-6 pt-6 border-t border-gray-200">
          <h3 className="text-sm font-medium text-gray-500 mb-2">How it works:</h3>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• Authentication is handled by Auth API (port 8081)</li>
            <li>• Session is stored in HTTP-only cookie</li>
            <li>• Access token automatically refreshes when expired</li>
            <li>• Session rotates periodically for security</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default ReportPage;