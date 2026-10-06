"use client";

import Image from "next/image";
import { useState } from "react";
import type { Media } from "@/lib/types";

/** 스크린샷 썸네일 목록. 썸네일을 누르면 위쪽 큰 이미지가 바뀐다. */
export function MediaGallery({ screenshots, title }: { screenshots: Media[]; title: string }) {
  const [selected, setSelected] = useState(0);
  if (screenshots.length === 0) return null;

  const current = screenshots[Math.min(selected, screenshots.length - 1)];

  return (
    <div className="space-y-3">
      <div className="relative aspect-video overflow-hidden rounded-lg bg-surface">
        <Image
          src={current.url}
          alt={`${title} 스크린샷 ${selected + 1}`}
          fill
          sizes="(min-width: 1152px) 1120px, 100vw"
          className="object-contain"
        />
      </div>
      {screenshots.length > 1 && (
        <ul className="flex gap-2 overflow-x-auto pb-1">
          {screenshots.map((shot, index) => (
            <li key={shot.url} className="shrink-0">
              <button
                type="button"
                onClick={() => setSelected(index)}
                aria-label={`스크린샷 ${index + 1} 보기`}
                aria-pressed={index === selected}
                className={`relative block h-[68px] w-[120px] overflow-hidden rounded border-2 ${
                  index === selected ? "border-accent" : "border-transparent opacity-70 hover:opacity-100"
                }`}
              >
                <Image src={shot.thumbnailUrl ?? shot.url} alt="" fill sizes="120px" className="object-cover" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
