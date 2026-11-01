/**
 * EditShare — Client Application Logic
 */

// Application State
let currentClip = null;
let currentClipId = null;
let activeViewPassword = null;
let activeCollabEditPassword = null;
let isEditing = false;
let qrCodeInstance = null;

// Local Storage Key for Creator Edit Tokens
const TOKEN_STORAGE_KEY = 'editshare_tokens';

// DOM Element References
const brandLogo = document.getElementById('brandLogo');
const navNewClipBtn = document.getElementById('navNewClipBtn');

// Panels
const createSection = document.getElementById('createSection');
const viewSection = document.getElementById('viewSection');
const passwordPromptSection = document.getElementById('passwordPromptSection');
const notFoundSection = document.getElementById('notFoundSection');

// Create Section Elements
const createTitle = document.getElementById('createTitle');
const createContent = document.getElementById('createContent');
const createLanguage = document.getElementById('createLanguage');
const createEditMode = document.getElementById('createEditMode');
const createExpiration = document.getElementById('createExpiration');
const toggleSecurityOptionsBtn = document.getElementById('toggleSecurityOptionsBtn');
const securityDrawer = document.getElementById('securityDrawer');
const createViewPassword = document.getElementById('createViewPassword');
const collabPasswordField = document.getElementById('collabPasswordField');
const createEditPassword = document.getElementById('createEditPassword');
const createClipBtn = document.getElementById('createClipBtn');
const charCount = document.getElementById('charCount');
const lineCount = document.getElementById('lineCount');

// View Section Elements
const viewClipTitle = document.getElementById('viewClipTitle');
const viewClipUrlDisplay = document.getElementById('viewClipUrlDisplay');
const viewLangBadge = document.getElementById('viewLangBadge');
const viewModeBadge = document.getElementById('viewModeBadge');
const viewVersionBadge = document.getElementById('viewVersionBadge');
const viewUpdatedBadge = document.getElementById('viewUpdatedBadge');
const readOnlyContainer = document.getElementById('readOnlyContainer');
const editableContainer = document.getElementById('editableContainer');
const codeViewer = document.getElementById('codeViewer');
const readOnlyLangLabel = document.getElementById('readOnlyLangLabel');
const editContentTextarea = document.getElementById('editContentTextarea');
const inplaceEditBar = document.getElementById('inplaceEditBar');
const editTitleInput = document.getElementById('editTitleInput');
const editLanguageSelect = document.getElementById('editLanguageSelect');
const editExpirationSelect = document.getElementById('editExpirationSelect');
const copyLinkBtn = document.getElementById('copyLinkBtn');
const shareClipBtn = document.getElementById('shareClipBtn');
const showQrBtn = document.getElementById('showQrBtn');
const versionHistoryBtn = document.getElementById('versionHistoryBtn');
const historyCount = document.getElementById('historyCount');
const editToggleBtn = document.getElementById('editToggleBtn');
const deleteClipBtn = document.getElementById('deleteClipBtn');
const cancelEditBtn = document.getElementById('cancelEditBtn');
const saveEditBtn = document.getElementById('saveEditBtn');
const copyRawContentBtn = document.getElementById('copyRawContentBtn');

// Modals
const clipShareCard = document.getElementById('clipShareCard');
const shareCardQrContainer = document.getElementById('shareCardQrContainer');
const shareCardTitle = document.getElementById('shareCardTitle');
const shareCardUrlLink = document.getElementById('shareCardUrlLink');
const shareCardCopyBtn = document.getElementById('shareCardCopyBtn');
const shareCardDownloadQrBtn = document.getElementById('shareCardDownloadQrBtn');
const shareCardShareBtn = document.getElementById('shareCardShareBtn');

const qrModal = document.getElementById('qrModal');
const closeQrModal = document.getElementById('closeQrModal');
const qrcodeContainer = document.getElementById('qrcodeContainer');
const qrUrlDisplay = document.getElementById('qrUrlDisplay');
const downloadQrBtn = document.getElementById('downloadQrBtn');
const copyQrLinkBtn = document.getElementById('copyQrLinkBtn');

const historyModal = document.getElementById('historyModal');
const closeHistoryModal = document.getElementById('closeHistoryModal');
const versionList = document.getElementById('versionList');
const previewVersionTitle = document.getElementById('previewVersionTitle');
const versionCodePreview = document.getElementById('versionCodePreview');
const revertBtn = document.getElementById('revertBtn');
let selectedVersionNumber = null;
let userExplicitlyChangedExpiration = false;

const collabPassModal = document.getElementById('collabPassModal');
const closeCollabPassModal = document.getElementById('closeCollabPassModal');
const collabEditPassInput = document.getElementById('collabEditPassInput');
const submitCollabPassBtn = document.getElementById('submitCollabPassBtn');

const viewPasswordInput = document.getElementById('viewPasswordInput');
const submitPasswordBtn = document.getElementById('submitPasswordBtn');
const passwordError = document.getElementById('passwordError');
const toastContainer = document.getElementById('toastContainer');

// ============================================================================
// Initialization & Routing
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {
  initRouting();
  setupEventListeners();
});

window.addEventListener('popstate', () => {
  initRouting();
});

window.addEventListener('pageshow', (event) => {
  if (event.persisted) {
    initRouting();
  }
});

function getPublicClipUrl(clip) {
  const origin = window.location.origin;
  const identifier = clip ? (clip.slug || clip.id) : currentClipId;
  if (!identifier) return origin;
  return origin + '/' + identifier;
}

function initRouting() {
  const path = window.location.pathname;
  const urlParams = new URLSearchParams(window.location.search);
  const tokenParam = urlParams.get('token') || urlParams.get('editToken');

  let slug = null;
  if (path.startsWith('/c/')) {
    slug = path.replace(/^\/c\//, '').replace(/\/.*$/, '').trim();
  } else if (path !== '/' && !path.startsWith('/api/') && !path.includes('.')) {
    slug = path.replace(/^\//, '').replace(/\/.*$/, '').trim();
  }

  if (slug) {
    if (tokenParam) {
      saveClipTokenLocally(slug, tokenParam);
    }
    loadClip(slug);
    return;
  }

  // Otherwise show creation view
  showView('create');
}

function showView(viewName) {
  createSection.classList.add('hidden');
  viewSection.classList.add('hidden');
  passwordPromptSection.classList.add('hidden');
  notFoundSection.classList.add('hidden');

  if (viewName === 'create') {
    createSection.classList.remove('hidden');
    resetCreateForm();
  } else if (viewName === 'view') {
    viewSection.classList.remove('hidden');
  } else if (viewName === 'password') {
    passwordPromptSection.classList.remove('hidden');
    viewPasswordInput.value = '';
    viewPasswordInput.focus();
  } else if (viewName === 'notfound') {
    notFoundSection.classList.remove('hidden');
  }
}

// ============================================================================
// Event Listeners
// ============================================================================

function setupEventListeners() {
  // Navigation
  brandLogo.addEventListener('click', (e) => {
    e.preventDefault();
    history.pushState({}, '', '/');
    showView('create');
  });

  navNewClipBtn.addEventListener('click', () => {
    history.pushState({}, '', '/');
    showView('create');
  });

  // Editor Tab Key Indentation
  createContent.addEventListener('keydown', handleEditorKeydown);
  editContentTextarea.addEventListener('keydown', handleEditorKeydown);

  // Editor Stats
  createContent.addEventListener('input', updateCharStats);

  // Toggle Security Options Drawer
  toggleSecurityOptionsBtn.addEventListener('click', () => {
    securityDrawer.classList.toggle('hidden');
  });

  createEditMode.addEventListener('change', () => {
    if (createEditMode.value === 'COLLABORATIVE') {
      collabPasswordField.style.display = 'block';
    } else {
      collabPasswordField.style.display = 'none';
    }
  });

  // Submit New Clip
  createClipBtn.addEventListener('click', handleCreateClip);

  // Keyboard shortcut Ctrl+S / Cmd+S to submit
  window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault();
      if (!createSection.classList.contains('hidden')) {
        handleCreateClip();
      } else if (isEditing) {
        handleSaveEdit();
      }
    }

    // Pressing Enter when on shared view page (not in an input/modal/editing) triggers edit mode
    if (e.key === 'Enter' && !isEditing && !viewClipSection.classList.contains('hidden')) {
      const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
      if (activeTag !== 'input' && activeTag !== 'textarea' && activeTag !== 'select' && activeTag !== 'button') {
        if (qrModal.classList.contains('hidden') && historyModal.classList.contains('hidden') && collabPassModal.classList.contains('hidden')) {
          e.preventDefault();
          handleToggleEdit();
        }
      }
    }
  });

  // Password Unlock
  submitPasswordBtn.addEventListener('click', handleUnlockPassword);
  viewPasswordInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') handleUnlockPassword();
  });

  // View Actions
  copyLinkBtn.addEventListener('click', () => {
    const url = getPublicClipUrl(currentClip);
    copyToClipboard(url);
    showToast('Public clip link copied to clipboard!', 'success');
  });

  if (shareCardCopyBtn) {
    shareCardCopyBtn.addEventListener('click', () => {
      const url = getPublicClipUrl(currentClip);
      copyToClipboard(url);
      showToast('Public clip link copied to clipboard!', 'success');
    });
  }

  if (shareCardDownloadQrBtn) {
    shareCardDownloadQrBtn.addEventListener('click', downloadQrImage);
  }

  if (shareCardShareBtn) {
    shareCardShareBtn.addEventListener('click', handleShareClip);
  }

  if (shareClipBtn) {
    shareClipBtn.addEventListener('click', handleShareClip);
  }

  copyRawContentBtn.addEventListener('click', () => {
    if (currentClip) {
      copyToClipboard(currentClip.content);
      showToast('Raw content copied to clipboard!', 'success');
    }
  });

  showQrBtn.addEventListener('click', openQrModal);
  closeQrModal.addEventListener('click', () => qrModal.classList.add('hidden'));

  downloadQrBtn.addEventListener('click', downloadQrImage);
  copyQrLinkBtn.addEventListener('click', () => {
    const url = getPublicClipUrl(currentClip);
    copyToClipboard(url);
    showToast('Link copied: ' + url, 'success');
  });

  // In-Place Edit Actions & Keydown
  editToggleBtn.addEventListener('click', handleToggleEdit);
  cancelEditBtn.addEventListener('click', handleCancelEdit);
  saveEditBtn.addEventListener('click', handleSaveEdit);

  if (editTitleInput) {
    editTitleInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleSaveEdit();
      }
    });
  }

  if (editExpirationSelect) {
    editExpirationSelect.addEventListener('change', () => {
      userExplicitlyChangedExpiration = true;
    });
    editExpirationSelect.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleSaveEdit();
      }
    });
  }

  if (editContentTextarea) {
    editContentTextarea.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        handleSaveEdit();
      }
    });
  }

  // Delete Clip
  deleteClipBtn.addEventListener('click', handleDeleteClip);

  // Version History Modal
  versionHistoryBtn.addEventListener('click', openHistoryModal);
  closeHistoryModal.addEventListener('click', () => historyModal.classList.add('hidden'));
  revertBtn.addEventListener('click', handleRevertVersion);

  // Collab Password Modal
  closeCollabPassModal.addEventListener('click', () => collabPassModal.classList.add('hidden'));
  submitCollabPassBtn.addEventListener('click', () => {
    activeCollabEditPassword = collabEditPassInput.value.trim();
    collabPassModal.classList.add('hidden');
    enterEditMode();
  });

  if (collabEditPassInput) {
    collabEditPassInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        activeCollabEditPassword = collabEditPassInput.value.trim();
        collabPassModal.classList.add('hidden');
        enterEditMode();
      }
    });
  }
}

function handleEditorKeydown(e) {
  if (e.key === 'Tab') {
    e.preventDefault();
    const start = this.selectionStart;
    const end = this.selectionEnd;
    this.value = this.value.substring(0, start) + '    ' + this.value.substring(end);
    this.selectionStart = this.selectionEnd = start + 4;
    updateCharStats();
  }
}

function updateCharStats() {
  const text = createContent.value;
  const chars = text.length;
  const lines = text ? text.split('\n').length : 1;
  charCount.textContent = `${chars} character${chars === 1 ? '' : 's'}`;
  lineCount.textContent = `${lines} line${lines === 1 ? '' : 's'}`;
}

function resetCreateForm() {
  createTitle.value = '';
  createContent.value = '';
  createLanguage.value = 'plaintext';
  createEditMode.value = 'PRIVATE';
  createExpiration.value = 'ONE_DAY';
  createViewPassword.value = '';
  createEditPassword.value = '';
  collabPasswordField.style.display = 'none';
  securityDrawer.classList.add('hidden');
  updateCharStats();
}

// ============================================================================
// API Calls: Create Clip
// ============================================================================

async function handleCreateClip() {
  const content = createContent.value.trim();
  if (!content) {
    showToast('Please enter some text or code before sharing.', 'error');
    createContent.focus();
    return;
  }

  const expirationVal = createExpiration.value;
  if (!expirationVal) {
    showToast('Please select a valid expiration duration.', 'error');
    createExpiration.focus();
    return;
  }

  const payload = {
    title: createTitle.value.trim() || 'Untitled Clip',
    content: createContent.value,
    language: createLanguage.value,
    editMode: createEditMode.value,
    expiration: expirationVal,
    viewPassword: createViewPassword.value.trim() || null,
    editPassword: createEditPassword.value.trim() || null
  };

  createClipBtn.disabled = true;
  createClipBtn.textContent = 'Generating Link...';

  try {
    const res = await fetch('/api/v1/clips', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      if (res.status === 409) {
        createTitle.focus();
        createTitle.select();
      }
      throw new Error(data.message || 'Failed to create clip');
    }

    const clip = data.data;

    // Securely save creator edit token locally
    saveClipTokenLocally(clip.id, clip.editToken);
    if (clip.slug) {
      saveClipTokenLocally(clip.slug, clip.editToken);
    }

    currentClip = clip;
    currentClipId = clip.slug || clip.id;
    showToast('Clip created! Unique link generated.', 'success');
    history.pushState({}, '', '/' + (clip.slug || clip.id));
    renderClipView(currentClip);
    showView('view');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    createClipBtn.disabled = false;
    createClipBtn.innerHTML = '<span>🚀</span> Create & Share Clip';
  }
}

// ============================================================================
// API Calls: Load & View Clip
// ============================================================================

async function loadClip(clipId) {
  currentClipId = clipId;
  currentClip = null; // Always clear previous in-memory content to prevent stale render
  const token = getSavedToken(clipId);

  const headers = {
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache'
  };
  if (token) headers['X-Edit-Token'] = token;
  if (activeViewPassword) headers['X-View-Password'] = activeViewPassword;

  try {
    const res = await fetch(`/api/v1/clips/${clipId}?_=${Date.now()}`, {
      headers,
      cache: 'no-store'
    });
    const data = await res.json();

    if (res.status === 401 || (data.message && data.message.includes('password protected'))) {
      currentClip = null;
      showView('password');
      return;
    }

    if (res.status === 404 || !res.ok || !data.success) {
      currentClip = null;
      codeViewer.textContent = '';
      showToast(data.message || 'Clip not found or expired', 'error');
      showView('notfound');
      return;
    }

    currentClip = data.data;
    renderClipView(currentClip);
    showView('view');
  } catch (err) {
    console.error('Failed to load clip:', err);
    currentClip = null;
    codeViewer.textContent = '';
    showToast(err.message, 'error');
    showView('notfound');
  }
}

function renderClipView(clip) {
  viewClipTitle.textContent = clip.title || 'Untitled Clip';
  const fullUrl = getPublicClipUrl(clip);
  if (viewClipUrlDisplay) {
    viewClipUrlDisplay.href = fullUrl;
    viewClipUrlDisplay.textContent = fullUrl;
  }

  // Update prominent Clip Sharing Card
  if (shareCardTitle) {
    shareCardTitle.textContent = clip.title || 'Untitled Clip';
  }
  if (shareCardUrlLink) {
    shareCardUrlLink.href = fullUrl;
    shareCardUrlLink.textContent = fullUrl;
  }
  if (shareCardQrContainer) {
    shareCardQrContainer.innerHTML = '';
    try {
      new QRCode(shareCardQrContainer, {
        text: fullUrl,
        width: 130,
        height: 130,
        colorDark: '#0d1117',
        colorLight: '#ffffff',
        correctLevel: QRCode.CorrectLevel.H
      });
    } catch (qrErr) {
      console.warn('QR Code rendering warning:', qrErr);
    }
  }

  const lang = clip.language || 'plaintext';
  viewLangBadge.textContent = lang;
  readOnlyLangLabel.textContent = lang.toUpperCase();

  // Mode badge
  if (clip.editMode === 'PRIVATE') {
    viewModeBadge.textContent = '🔒 Private';
    viewModeBadge.className = 'badge badge-mode';
  } else {
    viewModeBadge.textContent = '👥 Collaborative';
    viewModeBadge.className = 'badge badge-lang';
  }

  // Version badge
  viewVersionBadge.textContent = `v${clip.currentVersion || 1}`;
  historyCount.textContent = clip.currentVersion || 1;

  // Updated time
  if (clip.updatedAt) {
    const updDate = new Date(clip.updatedAt);
    viewUpdatedBadge.textContent = `Updated: ${updDate.toLocaleDateString()} ${updDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  } else {
    viewUpdatedBadge.textContent = 'Updated recently';
  }

  // Syntax Highlighting Content with safe fallback
  codeViewer.textContent = clip.content || '';
  codeViewer.className = `language-${lang}`;
  try {
    if (window.Prism && typeof Prism.highlightElement === 'function') {
      Prism.highlightElement(codeViewer);
    }
  } catch (prismErr) {
    console.warn('Prism highlighting fallback:', prismErr);
  }

  // Setup Edit and Delete Button visibility based on mode & authorization
  const hasToken = !!(getSavedToken(clip.id) || (clip.slug && getSavedToken(clip.slug)));
  if (hasToken) {
    deleteClipBtn.classList.remove('hidden');
  } else {
    deleteClipBtn.classList.add('hidden');
  }

  if (clip.editMode === 'PRIVATE') {
    if (clip.canEdit || hasToken) {
      editToggleBtn.classList.remove('hidden');
      editToggleBtn.disabled = false;
      editToggleBtn.title = 'Edit this clip (or press Enter)';
    } else {
      // Different browser, incognito window, or different PC: Edit button hidden
      editToggleBtn.classList.add('hidden');
    }
  } else {
    // Collaborative mode: Edit button is visible
    editToggleBtn.classList.remove('hidden');
    editToggleBtn.disabled = false;
    if (clip.canEdit) {
      editToggleBtn.title = 'Edit this collaborative clip (or press Enter)';
    } else if (clip.hasEditPassword) {
      editToggleBtn.title = 'Enter collaborative edit password to edit';
    } else {
      editToggleBtn.title = 'Edit this collaborative clip';
    }
  }

  exitEditMode();
}

async function handleUnlockPassword() {
  const enteredPass = viewPasswordInput.value.trim();
  if (!enteredPass) {
    passwordError.textContent = 'Please enter the password.';
    passwordError.classList.remove('hidden');
    return;
  }

  try {
    const res = await fetch(`/api/v1/clips/${currentClipId}/verify-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password: enteredPass })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      activeViewPassword = enteredPass;
      passwordError.classList.add('hidden');
      loadClip(currentClipId);
    } else {
      passwordError.textContent = 'Incorrect password. Please try again.';
      passwordError.classList.remove('hidden');
    }
  } catch (err) {
    passwordError.textContent = err.message;
    passwordError.classList.remove('hidden');
  }
}

// ============================================================================
// In-Place Editing
// ============================================================================

function handleToggleEdit() {
  if (isEditing) {
    exitEditMode();
  } else {
    const hasToken = currentClip && !!(getSavedToken(currentClip.id) || (currentClip.slug && getSavedToken(currentClip.slug)));
    if (currentClip.editMode === 'PRIVATE' && !currentClip.canEdit && !hasToken) {
      showToast('This clip is private. Only the creator can edit.', 'error');
      return;
    }
    if (!currentClip.canEdit && currentClip.editMode === 'COLLABORATIVE' && currentClip.hasEditPassword) {
      collabEditPassInput.value = '';
      collabPassModal.classList.remove('hidden');
      collabEditPassInput.focus();
    } else {
      enterEditMode();
    }
  }
}

function enterEditMode() {
  isEditing = true;
  userExplicitlyChangedExpiration = false;
  readOnlyContainer.classList.add('hidden');
  editableContainer.classList.remove('hidden');
  inplaceEditBar.classList.remove('hidden');
  editToggleBtn.classList.add('hidden');

  editTitleInput.value = currentClip.title;
  editLanguageSelect.value = currentClip.language || 'plaintext';
  editContentTextarea.value = currentClip.content;
  if (editExpirationSelect) {
    if (currentClip.deleteAfterOneView) {
      editExpirationSelect.value = 'AFTER_ONE_VIEW';
    } else {
      editExpirationSelect.value = 'ONE_DAY';
    }
  }
  editContentTextarea.focus();
}

function exitEditMode() {
  isEditing = false;
  userExplicitlyChangedExpiration = false;
  readOnlyContainer.classList.remove('hidden');
  editableContainer.classList.add('hidden');
  inplaceEditBar.classList.add('hidden');
  editToggleBtn.classList.remove('hidden');
}

function handleCancelEdit() {
  exitEditMode();
}

async function handleSaveEdit() {
  const newContent = editContentTextarea.value.trim();
  if (!newContent) {
    showToast('Content cannot be empty.', 'error');
    return;
  }

  const token = getSavedToken(currentClipId);
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['X-Edit-Token'] = token;
  if (activeCollabEditPassword) headers['X-Edit-Password'] = activeCollabEditPassword;

  const payload = {
    title: editTitleInput.value.trim() || 'Untitled Clip',
    language: editLanguageSelect.value,
    content: editContentTextarea.value,
    expectedVersion: currentClip ? currentClip.currentVersion : undefined
  };

  // Change expiration only when the user explicitly selects another option
  if (userExplicitlyChangedExpiration && editExpirationSelect && editExpirationSelect.value) {
    payload.expiration = editExpirationSelect.value;
  }

  saveEditBtn.disabled = true;
  saveEditBtn.textContent = 'Saving...';

  try {
    const res = await fetch(`/api/v1/clips/${currentClipId}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to update clip');
    }

    currentClip = data.data;
    renderClipView(currentClip);

    if (payload.expiration) {
      const expLabels = {
        'TEN_MINUTES': '10 Minutes',
        'ONE_HOUR': '1 Hour',
        'ONE_DAY': '1 Day',
        'ONE_WEEK': '1 Week',
        'AFTER_ONE_VIEW': 'Delete after 1 view'
      };
      showToast(`Clip updated & expiration modified to: ${expLabels[payload.expiration] || payload.expiration}! (v${currentClip.currentVersion})`, 'success');
    } else {
      showToast(`Clip updated in-place (Now at v${currentClip.currentVersion})!`, 'success');
    }
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    saveEditBtn.disabled = false;
    saveEditBtn.innerHTML = '<span>💾</span> Save Changes';
  }
}

async function handleDeleteClip() {
  if (!confirm('Are you sure you want to permanently delete this clip?')) return;

  const token = getSavedToken(currentClipId);
  if (!token) {
    showToast('Only the creator can delete this clip.', 'error');
    return;
  }

  try {
    const res = await fetch(`/api/v1/clips/${currentClipId}`, {
      method: 'DELETE',
      headers: { 'X-Edit-Token': token }
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to delete clip');
    }

    removeClipTokenLocally(currentClipId);
    showToast('Clip deleted successfully.', 'success');
    history.pushState({}, '', '/');
    showView('create');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// ============================================================================
// Share Action
// ============================================================================

function handleShareClip() {
  const shareUrl = getPublicClipUrl(currentClip);
  const shareTitle = currentClip ? (currentClip.title || 'EditShare Clip') : 'EditShare Clip';

  if (navigator.share) {
    navigator.share({
      title: shareTitle,
      text: 'Check out this snippet on EditShare:',
      url: shareUrl
    }).catch(() => {});
  } else {
    copyToClipboard(shareUrl);
    showToast('Clip link copied to clipboard!', 'success');
  }
}

// ============================================================================
// Version History Modal
// ============================================================================

async function openHistoryModal() {
  versionList.innerHTML = '<li class="version-item">Loading versions...</li>';
  versionCodePreview.textContent = 'Select a version to preview its snapshot.';
  revertBtn.classList.add('hidden');
  historyModal.classList.remove('hidden');

  const headers = {};
  if (activeViewPassword) headers['X-View-Password'] = activeViewPassword;

  try {
    const res = await fetch(`/api/v1/clips/${currentClipId}/versions`, { headers });
    const data = await res.json();

    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to fetch version history');
    }

    const versions = data.data;
    renderVersionList(versions);
  } catch (err) {
    versionList.innerHTML = `<li class="version-item error-msg">${err.message}</li>`;
  }
}

function renderVersionList(versions) {
  versionList.innerHTML = '';
  if (!versions || versions.length === 0) {
    versionList.innerHTML = '<li class="version-item">No previous versions found.</li>';
    return;
  }

  versions.forEach((v, index) => {
    const li = document.createElement('li');
    li.className = 'version-item' + (index === 0 ? ' active' : '');
    const date = new Date(v.createdAt).toLocaleString();
    
    li.innerHTML = `
      <div class="version-item-title">
        <span>Version ${v.versionNumber}</span>
        <span class="badge ${v.editType === 'INITIAL' ? 'badge-lang' : 'badge-version'}">${v.editType}</span>
      </div>
      <div class="version-item-time">${date}</div>
    `;

    li.addEventListener('click', () => {
      document.querySelectorAll('.version-item').forEach(el => el.classList.remove('active'));
      li.classList.add('active');
      displayVersionPreview(v);
    });

    versionList.appendChild(li);

    if (index === 0) {
      displayVersionPreview(v);
    }
  });
}

function displayVersionPreview(version) {
  selectedVersionNumber = version.versionNumber;
  previewVersionTitle.textContent = `Version ${version.versionNumber} (${version.title})`;
  versionCodePreview.textContent = version.content;

  // Show revert button only if user has edit rights and version is not current
  if (currentClip && currentClip.canEdit && version.versionNumber !== currentClip.currentVersion) {
    revertBtn.classList.remove('hidden');
    revertBtn.textContent = `⏪ Revert to v${version.versionNumber}`;
  } else {
    revertBtn.classList.add('hidden');
  }
}

async function handleRevertVersion() {
  if (!selectedVersionNumber) return;
  if (!confirm(`Revert clip to contents of Version ${selectedVersionNumber}?`)) return;

  const token = getSavedToken(currentClipId);
  const headers = {};
  if (token) headers['X-Edit-Token'] = token;
  if (activeCollabEditPassword) headers['X-Edit-Password'] = activeCollabEditPassword;

  try {
    const res = await fetch(`/api/v1/clips/${currentClipId}/versions/${selectedVersionNumber}/revert`, {
      method: 'POST',
      headers
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to revert version');
    }

    currentClip = data.data;
    renderClipView(currentClip);
    historyModal.classList.add('hidden');
    showToast(`Clip reverted to snapshot of v${selectedVersionNumber}! New current version is v${currentClip.currentVersion}.`, 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// ============================================================================
// QR Code Generator
// ============================================================================

function openQrModal() {
  const fullUrl = getPublicClipUrl(currentClip);
  qrUrlDisplay.textContent = fullUrl;
  qrcodeContainer.innerHTML = '';

  qrCodeInstance = new QRCode(qrcodeContainer, {
    text: fullUrl,
    width: 220,
    height: 220,
    colorDark: '#0d1117',
    colorLight: '#ffffff',
    correctLevel: QRCode.CorrectLevel.H
  });

  qrModal.classList.remove('hidden');
}

function downloadQrImage() {
  if (!currentClip && !currentClipId) return;

  const url = getPublicClipUrl(currentClip);
  // Meaningful filename based on clip title slug
  const identifier = (currentClip && currentClip.slug) ? currentClip.slug : (currentClipId || 'clip');
  const filename = `${identifier}-qr.png`;

  // Create an off-screen container for rendering a clean 512x512 high-res QR code
  const tempContainer = document.createElement('div');
  tempContainer.style.position = 'fixed';
  tempContainer.style.top = '-9999px';
  tempContainer.style.left = '-9999px';
  tempContainer.style.opacity = '0';
  document.body.appendChild(tempContainer);

  const qrSize = 512;
  const padding = 48; // Generous white quiet zone padding for scanning reliability

  try {
    new QRCode(tempContainer, {
      text: url,
      width: qrSize,
      height: qrSize,
      colorDark: '#000000',
      colorLight: '#ffffff',
      correctLevel: QRCode.CorrectLevel.H
    });

    // Allow QRCode.js to render canvas/img
    setTimeout(() => {
      try {
        const qrCanvas = tempContainer.querySelector('canvas');
        const qrImg = tempContainer.querySelector('img');

        // Create the high-res canvas with quiet zone margin
        const exportCanvas = document.createElement('canvas');
        exportCanvas.width = qrSize + padding * 2;
        exportCanvas.height = qrSize + padding * 2;
        const ctx = exportCanvas.getContext('2d');

        // Solid white background
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, exportCanvas.width, exportCanvas.height);

        const sourceEl = qrCanvas || qrImg;
        if (sourceEl) {
          ctx.imageSmoothingEnabled = false;
          ctx.drawImage(sourceEl, padding, padding, qrSize, qrSize);

          const link = document.createElement('a');
          link.download = filename;
          link.href = exportCanvas.toDataURL('image/png');
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          showToast(`QR Code downloaded: ${filename}`, 'success');
        } else {
          fallbackDownload(filename);
        }
      } catch (e) {
        console.warn('High-res QR export error, falling back:', e);
        fallbackDownload(filename);
      } finally {
        if (tempContainer.parentNode) {
          document.body.removeChild(tempContainer);
        }
      }
    }, 60);
  } catch (err) {
    console.error('Failed to generate QR for download:', err);
    fallbackDownload(filename);
    if (tempContainer.parentNode) {
      document.body.removeChild(tempContainer);
    }
  }
}

function fallbackDownload(filename) {
  const container = document.getElementById('shareCardQrContainer') || document.getElementById('qrcodeContainer');
  if (!container) return;
  const img = container.querySelector('img') || container.querySelector('canvas');
  if (!img) return;

  const url = (img.tagName.toLowerCase() === 'img') ? img.src : img.toDataURL('image/png');
  const link = document.createElement('a');
  link.download = filename;
  link.href = url;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// ============================================================================
// Local Token Management (Creator Edit Tokens)
// ============================================================================

function getTokensMap() {
  try {
    return JSON.parse(localStorage.getItem(TOKEN_STORAGE_KEY)) || {};
  } catch (e) {
    return {};
  }
}

function saveClipTokenLocally(id, editToken) {
  if (!id || !editToken) return;
  const map = getTokensMap();
  map[id] = editToken;
  localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(map));
}

function removeClipTokenLocally(id) {
  if (!id) return;
  const map = getTokensMap();
  delete map[id];
  localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(map));
}

function getSavedToken(id) {
  if (!id) return null;
  return getTokensMap()[id] || null;
}

// ============================================================================
// Utilities
// ============================================================================

function copyToClipboard(text) {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text);
  } else {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.opacity = '0';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    document.execCommand('copy');
    document.body.removeChild(textArea);
  }
}

function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : '⚠️'}</span> <span>${message}</span>`;
  toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(8px)';
    toast.style.transition = 'all 0.25s ease-out';
    setTimeout(() => toast.remove(), 250);
  }, 3200);
}
