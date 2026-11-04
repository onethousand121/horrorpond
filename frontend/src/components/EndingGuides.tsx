import { YouTubeEmbed } from "@/components/YouTubeEmbed";
import { getDictionary, type Locale } from "@/lib/i18n";
import { textLang } from "@/lib/textLang";
import type { EndingGuide, EndingType } from "@/lib/types";

/**
 * 엔딩 영역 (이름·설명은 큐레이터 글: 영어 번역이 있으면 영어 화면에 영어, 없으면 한국어). 단일·멀티 모두 같은 모양으로 접어 둔다 (펼치기 전에는 엔딩이 몇 개인지도 알 수 없게).
 * 단일 엔딩은 큐레이터가 엔딩 설명을 적었으면 그 설명을, 아니면 "엔딩이 하나" 안내를 보여 준다
 * (네이티브 details/summary라 JS 없이도 열리고 키보드로도 연다).
 */
export function EndingGuides({ endingType, endings, locale }: {
  endingType: EndingType;
  endings: EndingGuide[];
  locale: Locale;
}) {
  const t = getDictionary(locale).guide;
  // 멀티 엔딩인데 아직 목록이 없으면 보여줄 것이 없다
  if (endingType === "MULTIPLE" && endings.length === 0) return null;
  return (
    <section aria-label={t.endings} className="space-y-4">
      <hr className="divider" />
      <h2 className="font-pixel text-[22px]">{t.endings}</h2>
      <details className="group overflow-hidden rounded-xl border border-border bg-surface">
        <summary className="flex cursor-pointer list-none items-center gap-2 p-4 font-medium hover:bg-surface-2/60">
          <span aria-hidden className="text-accent transition group-open:rotate-90">▶</span>
          {t.endingSpoiler}
        </summary>
        {endingType === "SINGLE" && endings.length === 0 ? (
          <p className="border-t border-border p-4 text-sm text-muted">{t.singleEnding}</p>
        ) : (
          <ol className="divide-y divide-border border-t border-border">
            {endings.map((ending, index) => (
              <li key={`${ending.name}-${index}`} lang={textLang(ending.name, ending.description)} className="space-y-3 p-4">
                <div>
                  <h3 className="font-medium">{ending.name}</h3>
                  {ending.description && (
                    <p className="mt-0.5 text-sm whitespace-pre-line text-muted">{ending.description}</p>
                  )}
                </div>
                {ending.youtubeId && (
                  <YouTubeEmbed
                    youtubeId={ending.youtubeId}
                    title={t.endingVideo(ending.name)}
                    playLabel={t.play(t.endingVideo(ending.name))}
                  />
                )}
              </li>
            ))}
          </ol>
        )}
      </details>
    </section>
  );
}
