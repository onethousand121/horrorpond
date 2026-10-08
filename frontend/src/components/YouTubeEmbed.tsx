"use client";

import Image from "next/image";
import { useState } from "react";

/**
 * 가벼운 유튜브 플레이어: 처음에는 썸네일만 보여주고, 누르면 그때 플레이어(iframe)를 불러온다.
 * 쿠키를 덜 쓰는 youtube-nocookie 도메인으로 임베드한다.
 */
export function YouTubeEmbed({ youtubeId, title, playLabel }: { youtubeId: string; title: string; playLabel: string }) {
  const [playing, setPlaying] = useState(false);

  if (playing) {
    return (
      <div className="relative aspect-video overflow-hidden rounded-xl border border-border bg-black">
        <iframe
          src={`https://www.youtube-nocookie.com/embed/${youtubeId}?autoplay=1&rel=0`}
          title={title}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
          className="absolute inset-0 size-full"
        />
      </div>
    );
  }
  return (
    <button
      type="button"
      onClick={() => setPlaying(true)}
      aria-label={playLabel}
      className="group relative block aspect-video w-full overflow-hidden rounded-xl border border-border bg-black"
    >
      <Image
        src={`https://i.ytimg.com/vi/${youtubeId}/hqdefault.jpg`}
        alt=""
        fill
        sizes="(min-width: 1024px) 760px, 100vw"
        className="object-cover opacity-80 transition group-hover:opacity-100"
      />
      <span className="absolute inset-0 flex items-center justify-center">
        <span className="flex size-16 items-center justify-center rounded-full bg-danger/90 shadow-lg transition group-hover:scale-105">
          <svg aria-hidden viewBox="0 0 24 24" className="ml-1 size-7 fill-white">
            <path d="M8 5v14l11-7z" />
          </svg>
        </span>
      </span>
    </button>
  );
}
