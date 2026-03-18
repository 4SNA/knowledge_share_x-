/* ============================================
   KnowledgeGraphX — Frontend Application
   ============================================ */

// ============================================
// Navigation & Routing
// ============================================
function navigateTo(view) {
    // Update URL hash
    window.location.hash = view;
}

function handleRoute() {
    const hash = window.location.hash.replace('#', '') || 'dashboard';
    const views = document.querySelectorAll('.view');
    const navItems = document.querySelectorAll('.nav-item');

    views.forEach(v => v.classList.remove('active'));
    navItems.forEach(n => n.classList.remove('active'));

    const targetView = document.getElementById('view-' + hash);
    const targetNav = document.querySelector(`.nav-item[data-view="${hash}"]`);

    if (targetView) targetView.classList.add('active');
    if (targetNav) targetNav.classList.add('active');

    // Load data for the view
    switch (hash) {
        case 'dashboard': loadDashboard(); break;
        case 'documents': loadDocuments(); break;
        case 'graph': loadGraph(); break;
        case 'history': loadHistory(); break;
    }
}

// Listen for hash changes
window.addEventListener('hashchange', handleRoute);

// Navigation click handlers
document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        navigateTo(item.dataset.view);
    });
});

// ============================================
// API Helpers
// ============================================
const API = {
    async get(url) {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },
    async post(url, data) {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.error || `HTTP ${response.status}`);
        }
        return response.json();
    },
    async upload(url, formData) {
        const response = await fetch(url, {
            method: 'POST',
            body: formData
        });
        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.error || `Upload failed`);
        }
        return response.json();
    },
    async delete(url) {
        const response = await fetch(url, { method: 'DELETE' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    }
};

// ============================================
// Toast Notifications
// ============================================
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// ============================================
// Dashboard
// ============================================
async function loadDashboard() {
    try {
        const stats = await API.get('/api/dashboard/stats');
        animateCounter('stat-documents', stats.totalDocuments);
        animateCounter('stat-indexed', stats.indexedDocuments);
        animateCounter('stat-chunks', stats.totalChunks);
        animateCounter('stat-queries', stats.totalQueries);

        // Update stat bars
        const statCards = document.querySelectorAll('.stat-card');
        const maxVal = Math.max(stats.totalDocuments, stats.totalChunks, stats.totalQueries, 1);
        const vals = [stats.totalDocuments, stats.indexedDocuments, stats.totalChunks, stats.totalQueries];
        statCards.forEach((card, i) => {
            const fill = card.querySelector('.stat-bar-fill');
            if (fill) {
                setTimeout(() => {
                    fill.style.width = Math.min(100, (vals[i] / maxVal) * 100) + '%';
                }, 200);
            }
        });

        // Load recent documents
        const docs = await API.get('/api/documents');
        renderRecentDocuments(docs.slice(0, 5));

        // Load recent queries
        const queries = await API.get('/api/query/history/recent');
        renderRecentQueries(queries.slice(0, 5));

    } catch (err) {
        console.error('Dashboard load error:', err);
    }
}

function animateCounter(elementId, target) {
    const el = document.getElementById(elementId);
    if (!el) return;

    const duration = 1000;
    const start = parseInt(el.textContent) || 0;
    const increment = (target - start) / (duration / 16);
    let current = start;

    const timer = setInterval(() => {
        current += increment;
        if ((increment >= 0 && current >= target) || (increment < 0 && current <= target)) {
            el.textContent = target.toLocaleString();
            clearInterval(timer);
        } else {
            el.textContent = Math.round(current).toLocaleString();
        }
    }, 16);
}

function renderRecentDocuments(docs) {
    const container = document.getElementById('recent-documents');
    if (!docs || docs.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                </svg>
                <p>No documents uploaded yet</p>
                <button class="btn btn-outline btn-sm" onclick="navigateTo('documents')">Upload Now</button>
            </div>`;
        return;
    }

    container.innerHTML = docs.map(doc => `
        <div class="doc-item">
            <div class="doc-icon ${doc.fileType}">${doc.fileType}</div>
            <div class="doc-info">
                <div class="doc-name">${escapeHtml(doc.fileName)}</div>
                <div class="doc-meta">${formatFileSize(doc.fileSize)} · ${formatDate(doc.uploadedAt)}</div>
            </div>
            <span class="doc-status ${doc.status.toLowerCase()}">${doc.status}</span>
        </div>
    `).join('');
}

function renderRecentQueries(queries) {
    const container = document.getElementById('recent-queries');
    if (!queries || queries.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
                    <circle cx="11" cy="11" r="8"/>
                    <path d="M21 21l-4.35-4.35"/>
                </svg>
                <p>No queries yet</p>
                <button class="btn btn-outline btn-sm" onclick="navigateTo('query')">Ask a Question</button>
            </div>`;
        return;
    }

    container.innerHTML = queries.map(q => `
        <div class="history-item" onclick="rerunQuery('${escapeHtml(q.queryText)}')">
            <div class="history-query">${escapeHtml(q.queryText)}</div>
            <div class="history-response">${escapeHtml(q.response || '')}</div>
            <div class="history-meta">
                <span>${formatDate(q.createdAt)}</span>
            </div>
        </div>
    `).join('');
}

// ============================================
// Documents
// ============================================
async function loadDocuments() {
    try {
        const docs = await API.get('/api/documents');
        renderDocumentList(docs);
    } catch (err) {
        console.error('Documents load error:', err);
        showToast('Failed to load documents', 'error');
    }
}

function renderDocumentList(docs) {
    const container = document.getElementById('documents-list');
    const countEl = document.getElementById('doc-count');
    countEl.textContent = `${docs.length} document${docs.length !== 1 ? 's' : ''}`;

    if (!docs || docs.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                </svg>
                <p>No documents in your knowledge base</p>
            </div>`;
        return;
    }

    container.innerHTML = docs.map(doc => `
        <div class="doc-item" id="doc-${doc.id}">
            <div class="doc-icon ${doc.fileType}">${doc.fileType}</div>
            <div class="doc-info">
                <div class="doc-name">${escapeHtml(doc.fileName)}</div>
                <div class="doc-meta">
                    ${formatFileSize(doc.fileSize)} · 
                    ${doc.chunkCount ? doc.chunkCount + ' chunks · ' : ''}
                    ${formatDate(doc.uploadedAt)}
                </div>
            </div>
            <span class="doc-status ${doc.status.toLowerCase()}">${doc.status}</span>
            <div class="doc-actions">
                <button class="doc-delete" onclick="deleteDocument(${doc.id})" title="Delete document">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    </svg>
                </button>
            </div>
        </div>
    `).join('');
}

// File Upload
const uploadZone = document.getElementById('upload-zone');
const fileInput = document.getElementById('file-input');

if (uploadZone) {
    uploadZone.addEventListener('click', () => fileInput.click());

    uploadZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadZone.classList.add('drag-over');
    });

    uploadZone.addEventListener('dragleave', () => {
        uploadZone.classList.remove('drag-over');
    });

    uploadZone.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadZone.classList.remove('drag-over');
        const files = e.dataTransfer.files;
        handleFiles(files);
    });
}

if (fileInput) {
    fileInput.addEventListener('change', () => {
        handleFiles(fileInput.files);
        fileInput.value = '';
    });
}

async function handleFiles(files) {
    for (const file of files) {
        await uploadFile(file);
    }
}

async function uploadFile(file) {
    const progressContainer = document.getElementById('upload-progress');
    const progressFill = document.getElementById('progress-fill');
    const progressText = document.getElementById('progress-text');

    progressContainer.style.display = 'block';
    progressFill.style.width = '10%';
    progressText.textContent = `Uploading ${file.name}...`;

    try {
        const formData = new FormData();
        formData.append('file', file);

        progressFill.style.width = '40%';
        progressText.textContent = `Processing ${file.name}...`;

        const result = await API.upload('/api/documents/upload', formData);

        progressFill.style.width = '100%';
        progressText.textContent = `${file.name} indexed successfully!`;

        showToast(`${file.name} uploaded and indexed (${result.chunkCount || 0} chunks)`, 'success');

        setTimeout(() => {
            progressContainer.style.display = 'none';
            progressFill.style.width = '0%';
        }, 2000);

        loadDocuments();

    } catch (err) {
        progressFill.style.width = '100%';
        progressFill.style.background = 'var(--gradient-danger)';
        progressText.textContent = `Failed: ${err.message}`;

        showToast(`Upload failed: ${err.message}`, 'error');

        setTimeout(() => {
            progressContainer.style.display = 'none';
            progressFill.style.width = '0%';
            progressFill.style.background = '';
        }, 3000);
    }
}

async function deleteDocument(id) {
    if (!confirm('Delete this document and all its indexed data?')) return;

    try {
        await API.delete(`/api/documents/${id}`);
        showToast('Document deleted', 'success');
        loadDocuments();
    } catch (err) {
        showToast('Failed to delete document', 'error');
    }
}

// ============================================
// Query Interface
// ============================================
const queryInput = document.getElementById('query-input');
const chatArea = document.getElementById('chat-area');

if (queryInput) {
    queryInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submitQuery();
        }
    });

    // Auto-resize textarea
    queryInput.addEventListener('input', () => {
        queryInput.style.height = 'auto';
        queryInput.style.height = Math.min(queryInput.scrollHeight, 120) + 'px';
    });
}

function askSuggestion(chip) {
    queryInput.value = chip.textContent;
    submitQuery();
}

function rerunQuery(query) {
    navigateTo('query');
    setTimeout(() => {
        queryInput.value = query;
        submitQuery();
    }, 100);
}

async function submitQuery() {
    const query = queryInput.value.trim();
    if (!query) return;

    // Clear welcome screen
    const welcome = chatArea.querySelector('.chat-welcome');
    if (welcome) welcome.remove();

    // Add user message
    addChatMessage(query, 'user');
    queryInput.value = '';
    queryInput.style.height = 'auto';

    // Show typing indicator
    const typingId = addTypingIndicator();

    // Disable submit
    const submitBtn = document.getElementById('query-submit');
    submitBtn.disabled = true;

    try {
        const response = await API.post('/api/query', { query });

        // Remove typing indicator
        removeTypingIndicator(typingId);

        // Add AI response
        addAIResponse(response);

    } catch (err) {
        removeTypingIndicator(typingId);
        addChatMessage('Sorry, an error occurred while processing your query. Please try again.', 'ai');
        showToast('Query failed: ' + err.message, 'error');
    } finally {
        submitBtn.disabled = false;
    }
}

function addChatMessage(text, sender) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `chat-message ${sender}`;

    const avatar = sender === 'ai' ? 'AI' : 'U';
    const avatarClass = sender === 'ai' ? 'ai' : 'user';

    msgDiv.innerHTML = `
        <div class="chat-avatar ${avatarClass}">${avatar}</div>
        <div class="chat-bubble">${escapeHtml(text)}</div>
    `;

    chatArea.appendChild(msgDiv);
    chatArea.scrollTop = chatArea.scrollHeight;
}

function addAIResponse(response) {
    const msgDiv = document.createElement('div');
    msgDiv.className = 'chat-message ai';

    let confidenceClass = 'confidence-low';
    if (response.confidenceScore > 0.6) confidenceClass = 'confidence-high';
    else if (response.confidenceScore > 0.3) confidenceClass = 'confidence-medium';

    let sourcesHtml = '';
    if (response.sources && response.sources.length > 0) {
        sourcesHtml = `
            <div class="chat-sources">
                <div class="chat-sources-title">📄 Sources</div>
                ${response.sources.map(s => `
                    <div class="source-item">
                        <span class="source-name">${escapeHtml(s.documentName)}</span>
                        <span>Chunk ${s.chunkIndex}</span>
                        <span class="source-score">${(s.relevanceScore * 100).toFixed(1)}%</span>
                    </div>
                `).join('')}
            </div>
        `;
    }

    let suggestionsHtml = '';
    if (response.suggestions && response.suggestions.length > 0) {
        suggestionsHtml = `
            <div class="chat-suggestions">
                ${response.suggestions.map(s => `
                    <button class="chip" onclick="askSuggestion(this)">${escapeHtml(s)}</button>
                `).join('')}
            </div>
        `;
    }

    msgDiv.innerHTML = `
        <div class="chat-avatar ai">AI</div>
        <div class="chat-bubble">
            ${escapeHtml(response.answer)}
            <div class="chat-confidence ${confidenceClass}">
                Confidence: ${(response.confidenceScore * 100).toFixed(0)}%
            </div>
            ${sourcesHtml}
            ${suggestionsHtml}
        </div>
    `;

    chatArea.appendChild(msgDiv);
    chatArea.scrollTop = chatArea.scrollHeight;
}

let typingCounter = 0;

function addTypingIndicator() {
    const id = 'typing-' + (++typingCounter);
    const div = document.createElement('div');
    div.id = id;
    div.className = 'chat-message ai';
    div.innerHTML = `
        <div class="chat-avatar ai">AI</div>
        <div class="chat-bubble">
            <div class="typing-indicator">
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
            </div>
        </div>
    `;
    chatArea.appendChild(div);
    chatArea.scrollTop = chatArea.scrollHeight;
    return id;
}

function removeTypingIndicator(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

// ============================================
// Knowledge Graph (D3.js)
// ============================================
let graphSimulation = null;

async function loadGraph() {
    try {
        const data = await API.get('/api/graph');
        if (data.nodes && data.nodes.length > 0) {
            document.getElementById('graph-empty').style.display = 'none';
            renderGraph(data);
        } else {
            document.getElementById('graph-empty').style.display = 'flex';
        }
    } catch (err) {
        console.error('Graph load error:', err);
    }
}

function refreshGraph() {
    loadGraph();
    showToast('Knowledge graph refreshed', 'info');
}

function renderGraph(data) {
    const container = document.getElementById('graph-canvas');

    // Clear existing
    container.querySelectorAll('svg').forEach(s => s.remove());
    container.querySelectorAll('.graph-tooltip').forEach(t => t.remove());

    const width = container.clientWidth;
    const height = container.clientHeight || 520;

    const svg = d3.select(container)
        .append('svg')
        .attr('width', width)
        .attr('height', height);

    // Add zoom behavior
    const g = svg.append('g');
    svg.call(d3.zoom()
        .scaleExtent([0.3, 4])
        .on('zoom', (event) => g.attr('transform', event.transform)));

    // Tooltip
    const tooltip = document.createElement('div');
    tooltip.className = 'graph-tooltip';
    tooltip.style.display = 'none';
    container.appendChild(tooltip);

    // Create force simulation
    const simulation = d3.forceSimulation(data.nodes)
        .force('link', d3.forceLink(data.links)
            .id(d => d.id)
            .distance(d => 80 + (1 - d.weight) * 80))
        .force('charge', d3.forceManyBody().strength(-200))
        .force('center', d3.forceCenter(width / 2, height / 2))
        .force('collision', d3.forceCollide().radius(d => d.size + 5));

    graphSimulation = simulation;

    // Links
    const link = g.append('g')
        .selectAll('line')
        .data(data.links)
        .join('line')
        .attr('stroke', 'rgba(99, 102, 241, 0.2)')
        .attr('stroke-width', d => Math.max(1, d.weight * 3));

    // Node groups
    const node = g.append('g')
        .selectAll('g')
        .data(data.nodes)
        .join('g')
        .call(d3.drag()
            .on('start', dragStarted)
            .on('drag', dragged)
            .on('end', dragEnded));

    // Node circles with glow
    node.append('circle')
        .attr('r', d => d.size)
        .attr('fill', d => d.color)
        .attr('fill-opacity', 0.8)
        .attr('stroke', d => d.color)
        .attr('stroke-width', 2)
        .attr('stroke-opacity', 0.3)
        .style('filter', d => d.type === 'document' ? 'drop-shadow(0 0 8px ' + d.color + '40)' : 'none')
        .style('cursor', 'pointer');

    // Labels
    node.append('text')
        .text(d => d.label)
        .attr('text-anchor', 'middle')
        .attr('dy', d => d.size + 14)
        .attr('fill', '#94a3b8')
        .attr('font-size', d => d.type === 'document' ? '11px' : '9px')
        .attr('font-weight', d => d.type === 'document' ? '600' : '400')
        .style('pointer-events', 'none');

    // Hover effects
    node.on('mouseover', function(event, d) {
        d3.select(this).select('circle')
            .transition().duration(200)
            .attr('r', d.size * 1.3)
            .attr('fill-opacity', 1);

        tooltip.style.display = 'block';
        tooltip.textContent = `${d.type.toUpperCase()}: ${d.label}`;
        tooltip.style.left = (event.offsetX + 12) + 'px';
        tooltip.style.top = (event.offsetY - 30) + 'px';
    })
    .on('mouseout', function(event, d) {
        d3.select(this).select('circle')
            .transition().duration(200)
            .attr('r', d.size)
            .attr('fill-opacity', 0.8);

        tooltip.style.display = 'none';
    });

    // Tick
    simulation.on('tick', () => {
        link
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);

        node.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    function dragStarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }

    function dragEnded(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
}

// ============================================
// History
// ============================================
async function loadHistory() {
    try {
        const history = await API.get('/api/query/history');
        renderHistory(history);
    } catch (err) {
        console.error('History load error:', err);
    }
}

function renderHistory(history) {
    const container = document.getElementById('history-list');
    const clearBtn = document.getElementById('btn-clear-history');

    if (clearBtn) {
        clearBtn.style.display = history.length > 0 ? 'inline-flex' : 'none';
    }

    if (!history || history.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                </svg>
                <p>No query history yet</p>
            </div>`;
        return;
    }

    container.innerHTML = history.map(h => `
        <div class="history-item">
            <div class="history-query">${escapeHtml(h.queryText)}</div>
            <div class="history-response">${escapeHtml(h.response || '')}</div>
            <div class="history-meta">
                <span>${formatDate(h.createdAt)}</span>
                ${h.sources ? `<span>📄 ${escapeHtml(h.sources)}</span>` : ''}
                ${h.confidenceScore ? `<span>🎯 ${(h.confidenceScore * 100).toFixed(0)}%</span>` : ''}
                <button class="history-delete" onclick="deleteHistoryItem(event, ${h.id})" title="Delete">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"/>
                        <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>
            </div>
        </div>
    `).join('');
}

async function deleteHistoryItem(event, id) {
    event.stopPropagation();
    try {
        await API.delete(`/api/query/history/${id}`);
        loadHistory();
        showToast('History entry deleted', 'success');
    } catch (err) {
        showToast('Failed to delete history entry', 'error');
    }
}

async function clearAllHistory() {
    if (!confirm('Delete all query history?')) return;
    try {
        await API.delete('/api/query/history');
        loadHistory();
        showToast('All history cleared', 'success');
    } catch (err) {
        showToast('Failed to clear history', 'error');
    }
}

let searchTimeout = null;
async function searchHistory(keyword) {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(async () => {
        try {
            let history;
            if (keyword.trim()) {
                history = await API.get(`/api/query/history/search?keyword=${encodeURIComponent(keyword)}`);
            } else {
                history = await API.get('/api/query/history');
            }
            renderHistory(history);
        } catch (err) {
            console.error('Search error:', err);
        }
    }, 300);
}

// ============================================
// Utility Functions
// ============================================
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatFileSize(bytes) {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) {
        size /= 1024;
        i++;
    }
    return size.toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return Math.floor(diff / 60000) + 'm ago';
    if (diff < 86400000) return Math.floor(diff / 3600000) + 'h ago';
    if (diff < 604800000) return Math.floor(diff / 86400000) + 'd ago';

    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
    });
}

// ============================================
// Initialize
// ============================================
document.addEventListener('DOMContentLoaded', () => {
    handleRoute();
});

// Also handle initial load if DOM is already ready
if (document.readyState !== 'loading') {
    handleRoute();
}
