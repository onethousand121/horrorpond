"use client";

import { useActionState } from "react";
import { login } from "./actions";

export function LoginForm() {
  const [state, action, pending] = useActionState(login, null);
  return (
    <form action={action} className="space-y-3">
      <input
        type="password"
        name="key"
        autoComplete="current-password"
        placeholder="ADMIN_API_KEY"
        required
        className="w-full rounded border border-border bg-surface px-3 py-2"
      />
      <button
        type="submit"
        disabled={pending}
        className="w-full rounded bg-accent px-3 py-2 font-medium text-white disabled:opacity-50"
      >
        {pending ? "확인 중…" : "로그인"}
      </button>
      {state && !state.ok && <p className="text-sm text-red-400">{state.message}</p>}
    </form>
  );
}
