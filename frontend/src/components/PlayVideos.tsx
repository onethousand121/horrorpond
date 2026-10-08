"use client";

import Image from "next/image";
import { useState } from "react";
import { YouTubeEmbed } from "@/components/YouTubeEmbed";
import { getDictionary, type Locale } from "@/lib/i18n";
import type { PlayVideo } from "@/lib/types";

/** 플레이 영상: 큰 플레이어 1개 + 영상이 여러 개면 아래에 고르는 목록 */
export function PlayVideos({ videos, locale }: { videos: PlayVideo[]; locale: Locale }) {
  const t = getDictionary(locale).guide;
  const [active, setActive] = useState(0);
  const titleOf = (video: PlayVideo, index: number) => video.title ?? t.playVideoN(index + 1);
  const current = videos[active];
  if (!current) return null;

  return (
    <div className="space-y-3">
      <YouTubeEmbed
        key={current.youtubeId}
        youtubeId={current.youtubeId}
        title={titleOf(current, active)}
        playLabel={t.play(titleOf(current, active))}
      />
      {videos.length > 1 && (
        <ul aria-label={t.chooseVideo} className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {videos.map((video, index) => (
            <li key={`${video.youtubeId}-${index}`}>
              <button
                type="button"
                onClick={() => setActive(index)}
                aria-current={index === active ? "true" : undefined}
                className={`flex w-full gap-2 rounded-lg border p-1.5 text-left text-xs transition ${
                  index === active ? "border-accent bg-accent/10" : "border-border hover:border-accent/50"
                }`}
              >
                <span className="relative aspect-video w-24 shrink-0 overflow-hidden rounded bg-black">
                  <Image
                    src={`https://i.ytimg.com/vi/${video.youtubeId}/mqdefault.jpg`}
                    alt=""
                    fill
                    sizes="96px"
                    className="object-cover"
                  />
                </span>
                <span className="line-clamp-3 leading-snug">{titleOf(video, index)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
