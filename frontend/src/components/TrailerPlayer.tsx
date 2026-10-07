"use client";

import { useEffect, useRef, useState } from "react";
import type Hls from "hls.js";
import type { Media } from "@/lib/types";
import { getDictionary, type Locale } from "@/lib/i18n";

const HLS_MIME = "application/vnd.apple.mpegurl";

/**
 * Steam 트레일러(HLS m3u8) 재생기.
 * - poster는 headerImageUrl을 쓴다 (Steam 트레일러 썸네일은 저해상도).
 * - 재생 버튼을 누르기 전에는 영상도 hls.js도 불러오지 않는다.
 * - 브라우저가 HLS를 기본 지원하면(Safari, 최신 Chrome 등) hls.js 없이 video.src로 재생한다.
 */
export function TrailerPlayer({ trailers, poster, title, locale }: {
  trailers: Media[];
  poster: string | null;
  title: string;
  locale: Locale;
}) {
  const t = getDictionary(locale).media;
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [index, setIndex] = useState(0);
  const [started, setStarted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const current = trailers[index];

  useEffect(() => {
    const video = videoRef.current;
    if (!started || !video || !current) return;
    let cancelled = false;
    setError(null);

    async function attach(target: HTMLVideoElement, src: string) {
      if (target.canPlayType(HLS_MIME)) {
        target.src = src;
      } else {
        const { default: HlsClass } = await import("hls.js");
        if (cancelled) return;
        if (!HlsClass.isSupported()) {
          setError(t.unsupported);
          return;
        }
        const hls = new HlsClass();
        hlsRef.current = hls;
        hls.on(HlsClass.Events.ERROR, (_event, data) => {
          if (data.fatal) setError(t.failed);
        });
        hls.loadSource(src);
        hls.attachMedia(target);
      }
      target.play().catch(() => {
        // 자동 재생이 막히면 사용자가 컨트롤로 재생한다
      });
    }

    attach(video, current.url);

    return () => {
      cancelled = true;
      hlsRef.current?.destroy();
      hlsRef.current = null;
      video.removeAttribute("src");
      video.load();
    };
  }, [started, current, t]);

  if (!current) return null;

  return (
    <div className="space-y-2">
      <div className="relative aspect-video overflow-hidden rounded-lg bg-black">
        {started ? (
          <video
            ref={videoRef}
            controls
            playsInline
            poster={poster ?? undefined}
            className="h-full w-full"
            aria-label={t.trailer(title)}
          />
        ) : (
          <button
            type="button"
            onClick={() => setStarted(true)}
            className="group absolute inset-0 flex items-center justify-center"
            aria-label={t.playTrailer(title)}
          >
            {poster && (
              // poster는 Steam 헤더 이미지(460x215). 재생 전 정적 표시만 하므로 일반 img로 충분하다.
              // eslint-disable-next-line @next/next/no-img-element
              <img src={poster} alt="" className="absolute inset-0 h-full w-full object-cover opacity-60" />
            )}
            <span className="relative flex h-16 w-16 items-center justify-center rounded-full bg-accent/90 text-2xl text-white group-hover:bg-accent">
              ▶
            </span>
          </button>
        )}
      </div>
      {error && <p className="text-sm text-amber-300">{error}</p>}
      {trailers.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {trailers.map((trailer, i) => (
            <button
              key={trailer.url}
              type="button"
              onClick={() => {
                setIndex(i);
                setStarted(true);
              }}
              aria-pressed={i === index}
              className={`rounded border px-2.5 py-1 text-xs ${
                i === index ? "border-accent text-foreground" : "border-border text-muted hover:text-foreground"
              }`}
            >
              {t.trailerN(i + 1)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
