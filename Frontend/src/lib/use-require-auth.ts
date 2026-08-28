"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiRequestError, getCurrentUser, type CurrentUser } from "./api";

export function useRequireAuth() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();

    getCurrentUser(controller.signal)
      .then((currentUser) => {
        setUser(currentUser);
        setLoading(false);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        if (error instanceof ApiRequestError && error.status === 401) {
          router.replace("/");
          return;
        }
        setLoading(false);
      });

    return () => controller.abort();
  }, [router]);

  return { user, loading };
}
