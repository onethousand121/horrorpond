import Link from "next/link";
import { lang } from "next/root-params";
import { DEFAULT_LOCALE, getDictionary, isLocale, localePath } from "@/lib/i18n";

export default async function NotFound() {
  const value = await lang();
  const locale = value && isLocale(value) ? value : DEFAULT_LOCALE;
  const t = getDictionary(locale).notFound;
  return (
    <div className="mx-auto max-w-md space-y-4 py-16 text-center">
      <p className="text-5xl font-bold text-accent">404</p>
      <h1 className="text-xl font-bold">{t.title}</h1>
      <p className="text-muted">{t.body}</p>
      <Link
        href={localePath(locale, "/games")}
        className="inline-block rounded border border-border px-4 py-2 text-sm hover:border-foreground/40"
      >
        {t.browse}
      </Link>
    </div>
  );
}
