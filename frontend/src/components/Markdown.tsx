import ReactMarkdown, { type Components } from "react-markdown";

/**
 * 큐레이션 본문 렌더링. 원본 HTML은 렌더링하지 않는다(rehype-raw 미사용 → HTML은 무시됨).
 * 외부 링크는 새 탭에서 열고 opener/referrer를 넘기지 않는다.
 */
const components: Components = {
  a({ href, title, children }) {
    const external = href !== undefined && /^https?:\/\//i.test(href);
    return (
      <a
        href={href}
        title={title}
        {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})}
      >
        {children}
      </a>
    );
  },
};

export function Markdown({ children }: { children: string }) {
  return (
    <div className="markdown">
      <ReactMarkdown components={components}>{children}</ReactMarkdown>
    </div>
  );
}
