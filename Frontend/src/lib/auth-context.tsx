"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { ApiRequestError, getCurrentUser, logout as apiLogout, type CurrentUser } from "./api";

type AuthContextType = {
  user: CurrentUser | null;
  loading: boolean;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  logout: async () => {},
  refresh: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  async function fetchUser() {
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    } catch (error: unknown) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchUser();
  }, []);

  async function logout() {
    try {
      await apiLogout();
    } catch (error) {
      // ignore
    } finally {
      setUser(null);
      router.replace("/");
      router.refresh();
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        logout,
        refresh: fetchUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
