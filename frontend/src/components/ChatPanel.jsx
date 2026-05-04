import { useState, useRef, useEffect } from 'react'

export default function ChatPanel({ document }) {
  const [messages, setMessages] = useState([]) // { role: 'user'|'assistant', text: string }
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const bottomRef = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  useEffect(() => {
    setMessages([])
    setError(null)
    setQuestion('')
  }, [document?.id])

  const SUGGESTIONS = [
    'Summarize this CV',
    'What is their tech stack?',
    'What is their most recent role?',
    'What are their strongest skills?',
    'What is their educational background?',
  ]

  async function handleSend(override) {
    const q = (typeof override === 'string' ? override : question).trim()
    if (!q || !document || loading) return

    setMessages((prev) => [...prev, { id: Date.now(), role: 'user', text: q }])
    setQuestion('')
    setLoading(true)
    setError(null)

    try {
      const res = await fetch(`/api/documents/${document.id}/ask`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: q }),
      })

      if (!res.ok) {
        let message
        try {
          const err = await res.json()
          message = err.error || err.message || `Server error ${res.status}`
        } catch {
          message = await res.text() || `Server error ${res.status}`
        }
        throw new Error(message)
      }

      const data = await res.json()
      const answer = data.answer ?? data.response ?? JSON.stringify(data)
      setMessages((prev) => [...prev, { id: Date.now(), role: 'assistant', text: answer }])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
      inputRef.current?.focus()
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const disabled = !document || loading

  return (
    <div className="bg-white rounded-xl shadow-sm flex flex-col h-full overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-200 flex items-center gap-3 shrink-0">
        <h2 className="text-base font-semibold text-navy">Ask a Question</h2>
        {document && (
          <span className="text-xs bg-violet-100 text-violet-700 px-2 py-0.5 rounded-full font-medium max-w-[220px] overflow-hidden text-ellipsis whitespace-nowrap">
            {document.filename}
          </span>
        )}
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5 flex flex-col gap-4 min-h-0">
        {!document && (
          <div className="flex items-center justify-center h-full text-slate-400 text-[0.9375rem] text-center p-8">
            <p>Upload a CV on the left to start asking questions.</p>
          </div>
        )}

        {document && messages.length === 0 && !loading && (
          <div className="flex flex-col items-center justify-center h-full gap-5 p-8">
            <p className="text-slate-400 text-[0.9375rem] text-center">
              CV ready. Ask anything about <strong className="text-slate-600">{document.filename}</strong>.
            </p>
            <div className="flex flex-wrap justify-center gap-2">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => handleSend(s)}
                  className="px-3.5 py-1.5 rounded-full border border-indigo-200 bg-indigo-50
                    text-indigo-700 text-sm font-medium hover:bg-indigo-100 hover:border-indigo-400
                    transition-colors cursor-pointer"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex flex-col gap-1 max-w-[min(85%,48rem)]
              ${msg.role === 'user' ? 'self-end items-end' : 'self-start items-start'}`}
          >
            <span className="text-[0.6875rem] font-bold uppercase tracking-wide text-slate-400">
              {msg.role === 'user' ? 'You' : 'Assistant'}
            </span>
            <p className={`px-4 py-3 rounded-xl text-[0.9375rem] leading-relaxed whitespace-pre-wrap break-words
              ${msg.role === 'user'
                ? 'bg-indigo-600 text-white rounded-br-sm'
                : 'bg-slate-50 text-navy border border-slate-200 rounded-bl-sm'
              }`}
            >
              {msg.text}
            </p>
          </div>
        ))}

        {loading && (
          <div className="flex flex-col gap-1 self-start items-start">
            <span className="text-[0.6875rem] font-bold uppercase tracking-wide text-slate-400">Assistant</span>
            <div className="flex gap-1.5 px-4 py-3.5 bg-slate-50 border border-slate-200 rounded-xl rounded-bl-sm">
              <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce [animation-delay:0ms]" />
              <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce [animation-delay:150ms]" />
              <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce [animation-delay:300ms]" />
            </div>
          </div>
        )}

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-3.5 py-2.5 rounded-lg text-sm">
            <strong>Error:</strong> {error}
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {document && messages.length > 0 && (
        <div className="px-6 py-2.5 border-t border-slate-100 flex flex-wrap gap-2">
          {SUGGESTIONS.map((s) => (
            <button
              key={s}
              onClick={() => handleSend(s)}
              disabled={loading}
              className="px-3 py-1 rounded-full border border-indigo-200 bg-indigo-50
                text-indigo-700 text-xs font-medium hover:bg-indigo-100 hover:border-indigo-400
                transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      <div className="px-6 py-4 border-t border-slate-200 flex gap-3 items-end shrink-0">
        <textarea
          ref={inputRef}
          className="flex-1 resize-none border border-slate-300 rounded-lg px-3.5 py-2.5 text-[0.9375rem]
            font-[inherit] leading-relaxed outline-none transition-colors
            focus:border-indigo-600 focus:ring-2 focus:ring-indigo-600/10
            disabled:bg-slate-50 disabled:text-slate-400 disabled:cursor-not-allowed"
          placeholder={document ? 'Ask a question… (Enter to send)' : 'Upload a document first'}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          rows={2}
        />
        <button
          className="px-5 h-[42px] bg-indigo-600 text-white rounded-lg text-[0.9375rem] font-semibold
            flex items-center justify-center min-w-[72px] transition-colors
            hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={handleSend}
          disabled={disabled || !question.trim()}
        >
          {loading
            ? <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
            : 'Send'
          }
        </button>
      </div>
    </div>
  )
}
