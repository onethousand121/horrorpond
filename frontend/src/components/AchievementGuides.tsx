import { YouTubeEmbed } from "@/components/YouTubeEmbed";
import { getDictionary, type Locale } from "@/lib/i18n";
import type { AchievementGuide } from "@/lib/types";

/**
 * 업적 공략 목록. 공략 영상이 있는 업적은 펼치면 영상이 보인다(펼치기 전에는 영상을 불러오지 않는다).
 */
export function AchievementGuides({ achievements, locale }: { achievements: AchievementGuide[]; locale: Locale }) {
  const t = getDictionary(locale).guide;
  return (
    <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
      {achievements.map((achievement, index) => {
        const text = (
          <span className="min-w-0 flex-1">
            <span className="block font-medium">{achievement.name}</span>
            {achievement.description && (
              <span className="mt-0.5 block text-sm whitespace-pre-line text-muted">{achievement.description}</span>
            )}
          </span>
        );
        return (
          <li key={`${achievement.name}-${index}`}>
            {achievement.youtubeId ? (
              <details className="group">
                <summary className="flex cursor-pointer list-none items-start gap-3 p-4 hover:bg-surface-2/60">
                  {text}
                  <span className="shrink-0 rounded-full border border-accent/40 px-2 py-0.5 font-pixel text-[11px] text-accent">
                    ▶ {t.hasGuideVideo}
                  </span>
                </summary>
                <div className="px-4 pb-4">
                  <YouTubeEmbed
                    youtubeId={achievement.youtubeId}
                    title={achievement.name}
                    playLabel={t.play(achievement.name)}
                  />
                </div>
              </details>
            ) : (
              <div className="flex items-start gap-3 p-4">{text}</div>
            )}
          </li>
        );
      })}
    </ul>
  );
}
